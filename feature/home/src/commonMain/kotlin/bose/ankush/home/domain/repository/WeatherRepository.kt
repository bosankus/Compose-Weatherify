package bose.ankush.home.domain.repository

import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast
import kotlinx.coroutines.flow.Flow

internal interface WeatherRepository {
    fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality>

    fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?>

    suspend fun refreshWeatherData(
        coordinates: Pair<Double, Double>,
        forceRefresh: Boolean = false,
    )

    suspend fun clearAllData()
}
