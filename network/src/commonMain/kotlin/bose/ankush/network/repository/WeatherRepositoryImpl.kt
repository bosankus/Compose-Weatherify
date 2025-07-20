package bose.ankush.network.repository

import bose.ankush.network.api.WeatherApiService
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.model.AirQuality
import bose.ankush.network.model.WeatherForecast
import bose.ankush.network.utils.NetworkConstants
import bose.ankush.network.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Implementation of WeatherRepository
 */
class WeatherRepositoryImpl(
    private val apiService: WeatherApiService,
    private val networkConnectivity: NetworkConnectivity
) : WeatherRepository {

    // In-memory cache for weather data
    private val _weatherData = MutableStateFlow<WeatherForecast?>(null)
    private val _airQualityData = MutableStateFlow<AirQuality?>(null)

    // Last update timestamps
    private var lastWeatherUpdateTime: Long = 0
    private var lastAirQualityUpdateTime: Long = 0

    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality> {
        val currentTime = Clock.System.now().toEpochMilliseconds()

        // Check if we need to refresh the data
        if (_airQualityData.value == null || 
            (currentTime - lastAirQualityUpdateTime > NetworkConstants.CACHE_EXPIRATION_TIME)) {
            // Launch a coroutine to refresh the data
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    // Only refresh if network is available
                    if (networkConnectivity.isNetworkAvailable()) {
                        val airQualityData = NetworkUtils.retryWithExponentialBackoff {
                            apiService.getCurrentAirQuality(
                                latitude = coordinates.first.toString(),
                                longitude = coordinates.second.toString()
                            )
                        }
                        _airQualityData.value = airQualityData
                        lastAirQualityUpdateTime = currentTime
                    }
                } catch (e: Exception) {
                    // Log the error but don't throw it to avoid crashing the UI
                    println("Failed to refresh air quality data: ${e.message}")
                }
            }
        }

        // Initialize with a default AirQuality if null
        if (_airQualityData.value == null) {
            _airQualityData.value = AirQuality(data = null, message = null, status = null)
        }

        // Map the nullable flow to a non-nullable flow
        return _airQualityData.map { it ?: AirQuality(data = null, message = null, status = null) }
    }

    override fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<WeatherForecast?> {
        val currentTime = Clock.System.now().toEpochMilliseconds()

        // Check if we need to refresh the data
        if (_weatherData.value == null || 
            (currentTime - lastWeatherUpdateTime > NetworkConstants.CACHE_EXPIRATION_TIME)) {
            // Launch a coroutine to refresh the data
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    refreshWeatherData(coordinates)
                } catch (e: Exception) {
                    // Log the error but don't throw it to avoid crashing the UI
                    println("Failed to refresh weather data: ${e.message}")
                }
            }
        }

        return _weatherData.asStateFlow()
    }

    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val isNetworkAvailable = networkConnectivity.isNetworkAvailable()

        // Only fetch new data if:
        // 1. Network is available AND
        // 2. Either data is null OR data is stale (older than cache expiration time)
        if (isNetworkAvailable && 
            (_weatherData.value == null || (currentTime - lastWeatherUpdateTime > NetworkConstants.CACHE_EXPIRATION_TIME))) {
            try {
                coroutineScope {
                    // Use async to parallelize the API calls
                    val weatherDeferred = async {
                        NetworkUtils.retryWithExponentialBackoff {
                            apiService.getOneCallWeather(
                                coordinates.first.toString(),
                                coordinates.second.toString()
                            )
                        }
                    }

                    val airQualityDeferred = async {
                        NetworkUtils.retryWithExponentialBackoff {
                            apiService.getCurrentAirQuality(
                                latitude = coordinates.first.toString(),
                                longitude = coordinates.second.toString()
                            )
                        }
                    }

                    // Wait for both API calls to complete
                    val weatherData = weatherDeferred.await()
                    val airQualityData = airQualityDeferred.await()

                    // Update the weather data flow
                    _weatherData.value = weatherData
                    lastWeatherUpdateTime = currentTime

                    // Update the air quality data flow
                    _airQualityData.value = airQualityData
                    lastAirQualityUpdateTime = currentTime
                }
            } catch (e: Exception) {
                // If there's an error, throw a more descriptive exception
                throw Exception("Failed to refresh weather data: ${e.message}", e)
            }
        }
    }
}
