package bose.ankush.network.repository

import bose.ankush.network.model.WeatherForecast

interface WeatherRepository {
    /** Fetches fresh weather data from the network. Returns null if offline. */
    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>): WeatherForecast?
}
