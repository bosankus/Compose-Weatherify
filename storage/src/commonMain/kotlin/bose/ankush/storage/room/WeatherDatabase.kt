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
 * Version 6 adds the nullable `latitude`, `longitude`, and `placeName` columns: the place the
 * saved forecast belongs to.
 */
@Database(
    entities = [WeatherEntity::class, AirQualityEntity::class],
    version = 6,
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
