package bose.ankush.network.repository

import bose.ankush.network.model.WeatherForecast

interface WeatherRemoteSource {
    /** Fetches fresh weather data from the network. */
    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>): Result<WeatherForecast>
}
