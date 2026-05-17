package bose.ankush.storage.api

import kotlinx.coroutines.flow.Flow

interface WeatherStorage {
    fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<Any?>

    fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<Any?>

    suspend fun getLastWeatherUpdateTime(coordinates: Pair<Double, Double>): Long

    suspend fun saveLastWeatherUpdateTime(
        coordinates: Pair<Double, Double>,
        time: Long,
    )

    suspend fun saveWeatherData(
        weatherEntity: Any,
        airQualityEntity: Any,
    )

    suspend fun clearAllData()
}
