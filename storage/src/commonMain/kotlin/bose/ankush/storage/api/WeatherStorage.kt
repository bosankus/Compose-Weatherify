package bose.ankush.storage.api

import bose.ankush.storage.model.AirQualityData
import bose.ankush.storage.model.WeatherData
import kotlinx.coroutines.flow.Flow

interface WeatherStorage {
    fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<WeatherData?>

    fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQualityData?>

    suspend fun getLastWeatherUpdateTime(coordinates: Pair<Double, Double>): Long

    suspend fun saveLastWeatherUpdateTime(
        coordinates: Pair<Double, Double>,
        time: Long,
    )

    suspend fun saveWeatherData(
        weatherData: WeatherData,
        airQualityData: AirQualityData,
    )

    suspend fun clearAllData()
}
