package bose.ankush.storage.room

import android.content.Context
import androidx.room.Room
import bose.ankush.storage.common.WEATHER_DATABASE_NAME

/**
 * Builds the Android [WeatherDatabase] in SupportSQLite compatibility mode (no [androidx.sqlite.SQLiteDriver]).
 * Keeps the same on-disk file name ([WEATHER_DATABASE_NAME]) and Gson converters as before Room KMP.
 *
 * Registers every migration in [WEATHER_DATABASE_MIGRATIONS] (keep both builders in step): [MIGRATION_3_4]
 * (drop `auth_tokens` only), [MIGRATION_4_5] (add `timezoneOffset`) and [MIGRATION_5_6] (add `place_events`,
 * `sync_checkpoints`).
 * Destructive fallback stays disabled so weather
 * cache is never wiped on schema mismatch.
 */
fun createWeatherDatabase(
    context: Context,
    converters: WeatherDataModelConverters,
): WeatherDatabase =
    Room
        .databaseBuilder(
            context.applicationContext,
            WeatherDatabase::class.java,
            WEATHER_DATABASE_NAME,
        ).addTypeConverter(converters)
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .fallbackToDestructiveMigration(false)
        .build()
