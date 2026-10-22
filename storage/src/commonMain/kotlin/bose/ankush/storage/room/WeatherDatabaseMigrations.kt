package bose.ankush.storage.room

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import bose.ankush.storage.common.PLACE_EVENTS_TABLE
import bose.ankush.storage.common.SYNC_CHECKPOINTS_TABLE
import bose.ankush.storage.common.WEATHER_DATABASE_NAME

/**
 * Schema migrations for [WeatherDatabase].
 *
 * **3 → 4:** drop leftover `auth_tokens` only. Does not touch `central_weather_table` /
 * `central_aq_table`. Auth tokens are stored via EncryptedTokenStorage / Keychain.
 *
 * **4 → 5:** add the nullable `timezoneOffset` column (OpenWeather `timezone_offset`) to the
 * weather table. Existing rows read null and fall back to the device zone until the next refresh.
 *
 * **5 → 6:** create `place_events` (offline-first home place events) and `sync_checkpoints`
 * (the GET /place-events/sync cursor). Additive only; weather and air-quality rows are untouched.
 *
 * Both platform builders register every entry of [WEATHER_DATABASE_MIGRATIONS], with destructive fallback off.
 */
val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DROP TABLE IF EXISTS `auth_tokens`")
        }
    }

val MIGRATION_4_5: Migration =
    object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `$WEATHER_DATABASE_NAME` ADD COLUMN `timezoneOffset` INTEGER")
        }
    }

/** `CREATE TABLE` for [PlaceEventEntity]; must equal Room's generated SQL (see the schema test). */
internal const val CREATE_PLACE_EVENTS_SQL =
    "CREATE TABLE IF NOT EXISTS `$PLACE_EVENTS_TABLE` (" +
        "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `startsAt` TEXT NOT NULL, `note` TEXT, " +
        "`placeName` TEXT NOT NULL, `lat` REAL NOT NULL, `lon` REAL NOT NULL, " +
        "`createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `deletedAt` TEXT, " +
        "`syncState` TEXT NOT NULL, `serverUpdatedAt` TEXT, PRIMARY KEY(`id`))"

/** `CREATE TABLE` for [SyncCheckpointEntity]; must equal Room's generated SQL. */
internal const val CREATE_SYNC_CHECKPOINTS_SQL =
    "CREATE TABLE IF NOT EXISTS `$SYNC_CHECKPOINTS_TABLE` (" +
        "`stream` TEXT NOT NULL, `checkpoint` TEXT NOT NULL, PRIMARY KEY(`stream`))"

val MIGRATION_5_6: Migration =
    object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(CREATE_PLACE_EVENTS_SQL)
            connection.execSQL(CREATE_SYNC_CHECKPOINTS_SQL)
        }
    }

/** Every migration, oldest first. Both platform builders must register exactly these (checked by the schema test). */
val WEATHER_DATABASE_MIGRATIONS: List<Migration> = listOf(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
