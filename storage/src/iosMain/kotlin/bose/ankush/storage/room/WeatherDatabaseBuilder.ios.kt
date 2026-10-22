@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package bose.ankush.storage.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import bose.ankush.storage.common.WEATHER_DATABASE_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Builds a durable iOS [WeatherDatabase] with [BundledSQLiteDriver] under the app Documents directory.
 * File name matches Android's [WEATHER_DATABASE_NAME] for parity (fresh iOS DB; no cross-platform file share).
 *
 * Registers every migration in [WEATHER_DATABASE_MIGRATIONS] (keep both builders in step): [MIGRATION_3_4]
 * (drop `auth_tokens` only), [MIGRATION_4_5] (add `timezoneOffset`) and [MIGRATION_5_6] (add `place_events`,
 * `sync_checkpoints`).
 * Destructive fallback stays disabled.
 */
fun createWeatherDatabase(converters: WeatherDataModelConverters): WeatherDatabase {
    val dbPath = documentDirectory() + "/$WEATHER_DATABASE_NAME"
    return Room
        .databaseBuilder<WeatherDatabase>(name = dbPath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addTypeConverter(converters)
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .fallbackToDestructiveMigration(false)
        .build()
}

private fun documentDirectory(): String {
    val url =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(url?.path) { "NSDocumentDirectory path unavailable" }
}
