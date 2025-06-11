package bose.ankush.weatherify.data.room.weather

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WeatherEntity::class, AirQualityEntity::class],
    version = 2,
    autoMigrations = [AutoMigration (from = 1, to = 2)]
)
abstract class WeatherDatabase : RoomDatabase() {

    abstract fun weatherDao(): WeatherDao
}
