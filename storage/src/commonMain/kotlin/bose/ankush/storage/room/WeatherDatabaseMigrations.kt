package bose.ankush.storage.room

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import bose.ankush.storage.common.WEATHER_DATABASE_NAME

/**
 * Schema migrations for [WeatherDatabase].
 *
 * **3 → 4:** drop leftover `auth_tokens` only. Does not touch `central_weather_table` /
 * `central_aq_table`. Auth tokens are stored via EncryptedTokenStorage / Keychain.
 *
 * **4 → 5:** add the nullable `timezoneOffset` column (OpenWeather `timezone_offset`) to the
 * weather table. Existing rows read null and fall back to the device zone until the next refresh.
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
