package bose.ankush.network.repository

import bose.ankush.network.api.WeatherApiService
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.model.WeatherForecast
import bose.ankush.network.utils.NetworkConstants
import bose.ankush.network.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Implementation of WeatherRepository. Provides an in-memory cache for the unified weather
 * response, which now includes air quality and entitlements from a single /weather call.
 */
class WeatherRepositoryImpl(
    private val apiService: WeatherApiService,
    private val networkConnectivity: NetworkConnectivity,
) : WeatherRepository {
    private val weatherCache = MutableStateFlow<WeatherForecast?>(null)
    private var lastWeatherUpdateTime: Long = 0

    override fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<WeatherForecast?> {
        val currentTime = Clock.System.now().toEpochMilliseconds()

        if (weatherCache.value == null ||
            (currentTime - lastWeatherUpdateTime > NetworkConstants.CACHE_EXPIRATION_TIME)
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    refreshWeatherData(coordinates)
                } catch (e: Exception) {
                    println("Failed to refresh weather data: ${e.message}")
                }
            }
        }

        return weatherCache.asStateFlow()
    }

    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val isNetworkAvailable = networkConnectivity.isNetworkAvailable()

        if (isNetworkAvailable &&
            (
                    weatherCache.value == null ||
                            (currentTime - lastWeatherUpdateTime > NetworkConstants.CACHE_EXPIRATION_TIME)
                    )
        ) {
            try {
                val weatherData =
                    NetworkUtils.retryWithExponentialBackoff {
                        apiService.getOneCallWeather(
                            coordinates.first.toString(),
                            coordinates.second.toString(),
                        )
                    }
                weatherCache.value = weatherData
                lastWeatherUpdateTime = currentTime
            } catch (e: Exception) {
                throw Exception("Failed to refresh weather data: ${e.message}", e)
            }
        }
    }
}
