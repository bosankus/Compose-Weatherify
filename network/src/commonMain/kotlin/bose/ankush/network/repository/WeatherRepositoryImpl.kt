package bose.ankush.network.repository

import bose.ankush.network.api.WeatherApiService
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.model.WeatherForecast
import bose.ankush.network.utils.NetworkUtils

class WeatherRepositoryImpl(
    private val apiService: WeatherApiService,
    private val networkConnectivity: NetworkConnectivity,
) : WeatherRepository {
    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>): WeatherForecast? {
        if (!networkConnectivity.isNetworkAvailable()) return null

        return try {
            NetworkUtils.retryWithExponentialBackoff {
                apiService.getOneCallWeather(
                    coordinates.first.toString(),
                    coordinates.second.toString(),
                )
            }
        } catch (e: Exception) {
            throw Exception("Network | Failed to refresh weather data: ${e.message}", e)
        }
    }
}
