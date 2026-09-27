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
 */
@Database(
    entities = [WeatherEntity::class, AirQualityEntity::class],
    version = 4,
    exportSchema = false,
)
@ConstructedBy(WeatherDatabaseConstructor::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}

@Suppress("KotlinNoActualForExpect")
expect object WeatherDatabaseConstructor : RoomDatabaseConstructor<WeatherDatabase> {
    override fun initialize(): WeatherDatabase
}
