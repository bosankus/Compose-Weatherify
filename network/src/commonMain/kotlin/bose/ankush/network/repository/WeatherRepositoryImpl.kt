package bose.ankush.network.repository

import bose.ankush.network.api.WeatherApiService
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.common.NetworkException
import bose.ankush.network.model.WeatherForecast
import bose.ankush.network.utils.NetworkUtils

class WeatherRepositoryImpl(
    private val apiService: WeatherApiService,
    private val networkConnectivity: NetworkConnectivity,
) : WeatherRepository {
    override suspend fun refreshWeatherData(
        coordinates: Pair<Double, Double>,
    ): Result<WeatherForecast?> {
        if (!networkConnectivity.isNetworkAvailable()) {
            return Result.failure(
                NetworkException(
                    NetworkException.NETWORK_UNAVAILABLE,
                    "No internet connection available",
                ),
            )
        }

        return try {
            Result.success(
                NetworkUtils.retryWithExponentialBackoff {
                    apiService.getOneCallWeather(
                        coordinates.first.toString(),
                        coordinates.second.toString(),
                    )
                },
            )
        } catch (e: Exception) {
            // retryWithExponentialBackoff already normalizes failures to NetworkException.
            Result.failure(e)
        }
    }
}
