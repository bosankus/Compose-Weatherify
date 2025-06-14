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
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @return Flow of AirQuality
     */
    fun getAirQualityReport(latitude: String, longitude: String): Flow<AirQuality>

    /**
     * Get weather report for a location
     * @param location Pair of latitude and longitude
     * @return Flow of WeatherForecast
     */
    fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?>

    /**
     * Refresh weather data for a location
     * @param coordinates Pair of latitude and longitude
     */
    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>)
}