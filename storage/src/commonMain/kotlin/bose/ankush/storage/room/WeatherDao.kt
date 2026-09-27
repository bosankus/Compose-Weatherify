package bose.ankush.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import bose.ankush.storage.common.AQ_DATABASE_NAME
import bose.ankush.storage.common.WEATHER_DATABASE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Transaction
    suspend fun refreshWeather(
        weather: WeatherEntity,
        airQuality: AirQualityEntity,
    ) {
        deleteAllWeatherDetails()
        deleteAllAirQualityDetails()
        insertWeather(weather)
        insertAirQuality(airQuality)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAirQuality(airQuality: AirQualityEntity)

    @Query("SELECT * from $WEATHER_DATABASE_NAME")
    fun getWeather(): Flow<WeatherEntity?>

    @Query("SELECT * from $AQ_DATABASE_NAME")
    fun getAirQuality(): Flow<AirQualityEntity?>

    @Query("DELETE from $WEATHER_DATABASE_NAME")
    suspend fun deleteAllWeatherDetails()

    @Query("DELETE from $AQ_DATABASE_NAME")
    suspend fun deleteAllAirQualityDetails()

    @Transaction
    suspend fun clearAll() {
        deleteAllWeatherDetails()
        deleteAllAirQualityDetails()
    }
}
