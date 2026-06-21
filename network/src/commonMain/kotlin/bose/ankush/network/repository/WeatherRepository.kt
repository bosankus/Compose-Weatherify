package bose.ankush.network.repository

import bose.ankush.network.model.WeatherForecast

interface WeatherRepository {
    /**
     * Fetches fresh weather data from the network.
     *
     * Returns [Result.failure] with a [bose.ankush.network.common.NetworkException] when offline
     * or the request fails, so callers can surface the actual cause instead of a silent null.
     */
    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>): Result<WeatherForecast?>
}
