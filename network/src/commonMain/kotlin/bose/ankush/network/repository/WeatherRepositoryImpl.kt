package bose.ankush.network.repository

import bose.ankush.network.api.WeatherApiService
import bose.ankush.network.model.WeatherForecast
import bose.ankush.network.util.NetworkConnectivity
import bose.ankush.network.utils.NetworkUtils

class WeatherRepositoryImpl(
    private val apiService: WeatherApiService,
    private val networkConnectivity: NetworkConnectivity,
) : WeatherRepository {
    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>): Result<WeatherForecast> {
        if (!networkConnectivity.isNetworkAvailable()) {
            return Result.failure(Exception("No internet connection"))
        }

        return try {
            val data =
                NetworkUtils.retryWithExponentialBackoff {
                    apiService.getOneCallWeather(
                        coordinates.first.toString(),
                        coordinates.second.toString(),
                    )
                }
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
