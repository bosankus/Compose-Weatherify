package bose.ankush.weatherify.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * from central_weather_table")
    fun getWeather(): Flow<WeatherEntity>

    @Query("DELETE from central_weather_table")
    fun deleteAllWeatherDetails()
}