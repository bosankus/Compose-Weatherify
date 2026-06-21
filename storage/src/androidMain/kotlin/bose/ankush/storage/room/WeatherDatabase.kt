package bose.ankush.storage.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WeatherEntity::class, AirQualityEntity::class, AuthToken::class],
    version = 3,
    exportSchema = false,
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    abstract fun authTokenDao(): AuthTokenDao
}
