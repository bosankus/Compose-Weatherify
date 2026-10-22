package bose.ankush.storage.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * Shared Room database for durable weather / air-quality cache.
 *
 * Version 4 drops the unused `auth_tokens` table (tokens live in EncryptedSharedPreferences /
 * Keychain via [bose.ankush.storage.api.TokenStorage]). Weather / AQ tables are unchanged.
 * Version 5 adds the nullable `timezoneOffset` column to the weather table.
 * Version 6 adds `place_events` (offline-first home place events) and `sync_checkpoints`.
 *
 * The schema is exported to `storage/schemas` so migrations can be checked against it.
 */
@Database(
    entities = [
        WeatherEntity::class,
        AirQualityEntity::class,
        PlaceEventEntity::class,
        SyncCheckpointEntity::class,
    ],
    version = WEATHER_DATABASE_VERSION,
    exportSchema = true,
)
@ConstructedBy(WeatherDatabaseConstructor::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    abstract fun placeEventDao(): PlaceEventDao

    abstract fun syncCheckpointDao(): SyncCheckpointDao
}

/** Current [WeatherDatabase] schema version; [WEATHER_DATABASE_MIGRATIONS] must reach it. */
const val WEATHER_DATABASE_VERSION = 6

@Suppress("KotlinNoActualForExpect")
expect object WeatherDatabaseConstructor : RoomDatabaseConstructor<WeatherDatabase> {
    override fun initialize(): WeatherDatabase
}
