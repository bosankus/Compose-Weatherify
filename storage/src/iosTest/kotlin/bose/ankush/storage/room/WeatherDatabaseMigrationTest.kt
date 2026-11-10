@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package bose.ankush.storage.room

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import bose.ankush.storage.common.AQ_DATABASE_NAME
import bose.ankush.storage.common.WEATHER_DATABASE_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Opens a real schema-5 file through Room with [MIGRATION_5_6]. Room validates the migrated
 * tables against the entities on open, so a wrong column type or name fails here, not at launch.
 * Schemas are not exported, so the schema-5 tables are Room's own schema-6 SQL minus the columns
 * this migration adds.
 */
class WeatherDatabaseMigrationTest {
    private val files = mutableListOf<String>()

    @AfterTest
    fun deleteFiles() {
        files.forEach { base ->
            listOf("", "-wal", "-shm", "-journal").forEach {
                NSFileManager.defaultManager.removeItemAtPath(base + it, null)
            }
        }
    }

    @Test
    fun version5RowSurvivesWithNoPlace() =
        runBlocking {
            val db = open(createVersion5Database())
            try {
                val row = db.weatherDao().findWeather()
                assertEquals(LAST_UPDATED, row?.lastUpdated)
                assertEquals(TIMEZONE_OFFSET, row?.timezoneOffset)
                assertNull(row?.latitude)
                assertNull(row?.longitude)
                assertNull(row?.placeName)
            } finally {
                db.close()
            }
        }

    @Test
    fun migratedDatabaseSavesThePlace() =
        runBlocking {
            val db = open(createVersion5Database())
            try {
                db.weatherDao().refreshWeather(
                    WeatherEntity(
                        id = 2L,
                        lastUpdated = 9L,
                        latitude = 22.57,
                        longitude = 88.36,
                        placeName = "Kolkata"
                    ),
                    AirQualityEntity(aqi = 3),
                )
                val (weather, airQuality) = db.weatherDao().findWeatherWithAirQuality()!!
                assertEquals(22.57, weather.latitude)
                assertEquals(88.36, weather.longitude)
                assertEquals("Kolkata", weather.placeName)
                assertEquals(3, airQuality?.aqi)
            } finally {
                db.close()
            }
        }

    /** A schema-5 file holding one weather row, at `user_version` 5. */
    private fun createVersion5Database(): String {
        val reference = tempFile("reference")
        runBlocking {
            val db = open(reference)
            db.weatherDao().findWeather()
            db.close()
        }
        val driver = BundledSQLiteDriver()
        val (weatherSql, airQualitySql) =
            driver.open(reference).let { connection ->
                try {
                    connection.createSql(WEATHER_DATABASE_NAME) to connection.createSql(
                        AQ_DATABASE_NAME
                    )
                } finally {
                    connection.close()
                }
            }
        val version5WeatherSql =
            COLUMNS_ADDED_IN_6.fold(weatherSql) { sql, column ->
                sql.replace(", $column", "")
                    .also { assertNotEquals(sql, it, "Room no longer declares $column") }
            }

        val legacy = tempFile("v5")
        val connection = driver.open(legacy)
        try {
            connection.execSQL(version5WeatherSql)
            connection.execSQL(airQualitySql)
            connection.execSQL(
                "INSERT INTO `$WEATHER_DATABASE_NAME` (`id`, `lastUpdated`, `timezoneOffset`) " +
                        "VALUES (1, $LAST_UPDATED, $TIMEZONE_OFFSET)",
            )
            connection.execSQL("PRAGMA user_version = 5")
        } finally {
            connection.close()
        }
        return legacy
    }

    private fun open(path: String): WeatherDatabase =
        Room
            .databaseBuilder<WeatherDatabase>(name = path)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addTypeConverter(WeatherDataModelConverters())
            .addMigrations(MIGRATION_5_6)
            .fallbackToDestructiveMigration(false)
            .build()

    private fun tempFile(name: String): String =
        (NSTemporaryDirectory() + "weather-migration-$name-${
            Random.nextLong().toULong()
        }.db").also { files += it }

    private fun SQLiteConnection.createSql(table: String): String =
        prepare("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?").use { statement ->
            statement.bindText(1, table)
            check(statement.step()) { "No table $table" }
            statement.getText(0)
        }

    private companion object {
        const val LAST_UPDATED = 1_700_000_000_000L
        const val TIMEZONE_OFFSET = 19_800
        val COLUMNS_ADDED_IN_6 = listOf("`latitude` REAL", "`longitude` REAL", "`placeName` TEXT")
    }
}
