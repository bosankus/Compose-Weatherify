package bose.ankush.storage.api

import bose.ankush.storage.model.AirQualityData
import bose.ankush.storage.model.StoredWeather
import bose.ankush.storage.model.WeatherData
import kotlinx.coroutines.flow.Flow

interface WeatherStorage {
    /**
     * The saved forecast and its air quality, read together. Room keeps one global row, so
     * there are no coordinates to pass. Emits null when nothing is saved.
     */
    fun observeWeather(): Flow<StoredWeather?>

    suspend fun saveWeatherData(
        weatherData: WeatherData,
        airQualityData: AirQualityData,
    )

    suspend fun clearAllData()
}
