package bose.ankush.network.repository

import bose.ankush.network.model.WeatherForecast
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for weather data
 */
interface WeatherRepository {
    /**
     * Get unified weather report for a location.
     * The response includes current, hourly, daily, alerts, and air quality.
     * Premium-only fields are null for free tier users — check entitlements.upgradeRequired.
     * @param coordinates Pair of latitude and longitude
     * @return Flow of WeatherForecast
     */
    fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<WeatherForecast?>

    /**
     * Force a fresh fetch from the network for a location
     * @param coordinates Pair of latitude and longitude
     */
    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>)
}
