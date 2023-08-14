package bose.ankush.weatherify.data.room.weather

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import bose.ankush.weatherify.base.common.WEATHER_DATABASE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Transaction
    fun refreshWeather(weather: WeatherEntity) {
        deleteAllWeatherDetails()
        insertWeather(weather)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * from $WEATHER_DATABASE_NAME")
    fun getWeather(): Flow<WeatherEntity>

    @Query("DELETE from $WEATHER_DATABASE_NAME")
    fun deleteAllWeatherDetails()
}