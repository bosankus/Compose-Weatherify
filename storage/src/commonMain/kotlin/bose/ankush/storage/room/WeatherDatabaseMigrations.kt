package bose.ankush.storage.room

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Schema migrations for [WeatherDatabase].
 *
 * **3 → 4:** drop leftover `auth_tokens` only. Does not touch `central_weather_table` /
 * `central_aq_table`. Auth tokens are stored via EncryptedTokenStorage / Keychain.
 */
val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DROP TABLE IF EXISTS `auth_tokens`")
        }
    }
