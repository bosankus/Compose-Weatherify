package bose.ankush.network.api

import bose.ankush.network.model.AirQuality
import bose.ankush.network.model.WeatherForecast

/**
 * API service interface for weather data
 */
interface WeatherApiService {
    /**
     * Get current air quality for a location
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @return AirQuality
     */
    suspend fun getCurrentAirQuality(
        latitude: String,
        longitude: String
    ): AirQuality

    /**
     * Get weather forecast for a location
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @return WeatherForecast
     */
    suspend fun getOneCallWeather(
        latitude: String,
        longitude: String
    ): WeatherForecast
}