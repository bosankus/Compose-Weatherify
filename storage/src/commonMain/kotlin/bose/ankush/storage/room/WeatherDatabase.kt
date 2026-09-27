package bose.ankush.storage.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [WeatherEntity::class, AirQualityEntity::class, AuthToken::class],
    version = 3,
    exportSchema = false,
)
@ConstructedBy(WeatherDatabaseConstructor::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    abstract fun authTokenDao(): AuthTokenDao
}

@Suppress("KotlinNoActualForExpect")
expect object WeatherDatabaseConstructor : RoomDatabaseConstructor<WeatherDatabase> {
    override fun initialize(): WeatherDatabase
}
