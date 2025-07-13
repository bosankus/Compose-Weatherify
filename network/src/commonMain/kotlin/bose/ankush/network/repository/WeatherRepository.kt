package bose.ankush.network.repository

import bose.ankush.network.model.AirQuality
import bose.ankush.network.model.WeatherForecast
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for weather data
 */
interface WeatherRepository {
    /**
     * Get air quality report for a location
     * @param coordinates Pair of latitude and longitude
     * @return Flow of AirQuality
     */
    fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality>

    /**
     * Get weather report for a location
     * @param coordinates Pair of latitude and longitude
     * @return Flow of WeatherForecast
     */
    fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<WeatherForecast?>

    /**
     * Refresh weather data for a location
     * @param coordinates Pair of latitude and longitude
     */
    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>)
}
