package bose.ankush.network.api

import bose.ankush.network.model.WeatherForecast

/**
 * API service interface for weather data
 */
interface WeatherApiService {
    /**
     * Get unified weather data for a location (current, hourly, daily, alerts, air quality).
     * Air quality and premium-only fields are null for free tier users.
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @return WeatherForecast containing all available data for the user's subscription tier
     */
    suspend fun getOneCallWeather(
        latitude: String,
        longitude: String
    ): WeatherForecast
}
