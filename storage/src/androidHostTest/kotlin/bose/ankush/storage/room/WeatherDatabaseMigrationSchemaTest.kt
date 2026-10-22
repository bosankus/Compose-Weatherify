package bose.ankush.storage.room

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import bose.ankush.storage.common.PLACE_EVENTS_TABLE
import bose.ankush.storage.common.SYNC_CHECKPOINTS_TABLE
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Schema / SQL check for [MIGRATION_5_6]. Room's MigrationTestHelper needs an instrumented
 * SQLite, which host tests do not have, so this compares the SQL the migration runs with the
 * schema Room exports for version 6 (`storage/schemas`). Room validates a migrated table by its
 * columns, types, nullability and primary key, all of which are in that `createSql`.
 */
class WeatherDatabaseMigrationSchemaTest {
    private val schema: JsonObject by lazy {
        val file = File("schemas/bose.ankush.storage.room.WeatherDatabase/$WEATHER_DATABASE_VERSION.json")
        assertTrue(file.exists(), "Exported schema missing: ${file.absolutePath}")
        Json
            .parseToJsonElement(file.readText())
            .jsonObject
            .getValue("database")
            .jsonObject
    }

    @Test
    fun exportedSchemaIsTheCurrentVersion() {
        assertEquals(6, WEATHER_DATABASE_VERSION)
        assertEquals(WEATHER_DATABASE_VERSION, schema.getValue("version").jsonPrimitive.int)
    }

    @Test
    fun migrationsFormOneUnbrokenChainUpToTheCurrentVersion() {
        val steps = WEATHER_DATABASE_MIGRATIONS.map { it.startVersion to it.endVersion }

        assertEquals(listOf(3 to 4, 4 to 5, 5 to 6), steps)
        assertEquals(WEATHER_DATABASE_VERSION, steps.last().second)
    }

    @Test
    fun bothPlatformBuildersRegisterEveryMigration() {
        val expected = WEATHER_DATABASE_MIGRATIONS.joinToString { "MIGRATION_${it.startVersion}_${it.endVersion}" }
        listOf(
            "src/androidMain/kotlin/bose/ankush/storage/room/WeatherDatabaseBuilder.android.kt",
            "src/iosMain/kotlin/bose/ankush/storage/room/WeatherDatabaseBuilder.ios.kt",
        ).forEach { path ->
            val source = File(path).readText()
            assertTrue(".addMigrations($expected)" in source, "$path must call .addMigrations($expected)")
            assertTrue(".fallbackToDestructiveMigration(false)" in source, "$path must keep destructive fallback off")
        }
    }

    @Test
    fun migration5To6CreatesExactlyTheTablesRoomExpects() {
        val executed = mutableListOf<String>()

        MIGRATION_5_6.migrate(recordingConnection(executed))

        assertEquals(
            listOf(createSql(PLACE_EVENTS_TABLE), createSql(SYNC_CHECKPOINTS_TABLE)),
            executed,
        )
    }

    @Test
    fun placeEventsHasTheNullableSyncErrorColumnForFailedRows() {
        val syncError =
            entity(PLACE_EVENTS_TABLE)
                .getValue("fields")
                .jsonArray
                .map { it.jsonObject }
                .single { it.getValue("columnName").jsonPrimitive.content == "syncError" }

        assertEquals("TEXT", syncError.getValue("affinity").jsonPrimitive.content)
        assertEquals(false, syncError["notNull"]?.jsonPrimitive?.boolean ?: false)
        assertTrue("`syncError` TEXT," in CREATE_PLACE_EVENTS_SQL)
    }

    @Test
    fun migration5To6LeavesWeatherTablesAlone() {
        val executed = mutableListOf<String>()

        MIGRATION_5_6.migrate(recordingConnection(executed))

        assertTrue(executed.none { "central_weather_table" in it || "central_aq_table" in it })
        assertTrue(executed.none { it.startsWith("DROP") || it.startsWith("ALTER") })
    }

    @Test
    fun newTablesHaveNoIndicesOrForeignKeysToMigrate() {
        listOf(PLACE_EVENTS_TABLE, SYNC_CHECKPOINTS_TABLE).forEach { table ->
            val entity = entity(table)
            assertTrue(entity["indices"]?.jsonArray.isNullOrEmpty(), "$table indices")
            assertTrue(entity["foreignKeys"]?.jsonArray.isNullOrEmpty(), "$table foreign keys")
        }
    }

    private fun entity(table: String): JsonObject =
        schema
            .getValue("entities")
            .jsonArray
            .map { it.jsonObject }
            .first { it.getValue("tableName").jsonPrimitive.content == table }

    private fun createSql(table: String): String =
        entity(table)
            .getValue("createSql")
            .jsonPrimitive.content
            .replace("\${TABLE_NAME}", table)

    /** A connection that records every prepared statement; each statement steps once, empty. */
    private fun recordingConnection(executed: MutableList<String>): SQLiteConnection {
        val statement = proxy<SQLiteStatement> { _ -> null }
        return proxy { args ->
            executed += args?.firstOrNull() as String
            statement
        }
    }

    private inline fun <reified T> proxy(crossinline onPrepare: (Array<out Any?>?) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            when {
                method.name == "prepare" -> onPrepare(args)
                method.returnType == java.lang.Boolean.TYPE -> false
                method.returnType == Integer.TYPE -> 0
                method.returnType == java.lang.Long.TYPE -> 0L
                method.returnType == java.lang.Double.TYPE -> 0.0
                else -> null
            }
        } as T
}
