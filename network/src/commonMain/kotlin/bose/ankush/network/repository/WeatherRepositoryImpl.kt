package bose.ankush.network.repository

import bose.ankush.network.api.WeatherApiService
import bose.ankush.network.common.Constants
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.common.NetworkUtils
import bose.ankush.network.model.AirQuality
import bose.ankush.network.model.WeatherForecast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

    override fun getAirQualityReport(latitude: String, longitude: String): Flow<AirQuality> {
        // Initialize with a default AirQuality if null
        if (_airQualityData.value == null) {
            _airQualityData.value = AirQuality()
        }

        // Map the nullable flow to a non-nullable flow
        return _airQualityData.map { it ?: AirQuality() }
    }

    override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?> {
        return _weatherData.asStateFlow()
    }

    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val isNetworkAvailable = networkConnectivity.isNetworkAvailable()

        // Only fetch new data if:
        // 1. Network is available AND
        // 2. Either data is null OR data is stale (older than cache expiration time)
        if (isNetworkAvailable && 
            (_weatherData.value == null || (currentTime - lastWeatherUpdateTime > Constants.CACHE_EXPIRATION_TIME))) {
            try {
                // Fetch weather data with retry mechanism
                val weatherData = NetworkUtils.retryWithExponentialBackoff {
                    apiService.getOneCallWeather(
                        coordinates.first.toString(),
                        coordinates.second.toString()
                    )
                }

                // Update the weather data flow
                _weatherData.value = weatherData.copy(lastUpdated = currentTime)
                lastWeatherUpdateTime = currentTime

                // Fetch air quality data with retry mechanism
                val airQualityData = NetworkUtils.retryWithExponentialBackoff {
                    apiService.getCurrentAirQuality(
                        latitude = coordinates.first.toString(),
                        longitude = coordinates.second.toString()
                    )
                }

                // Update the air quality data flow
                _airQualityData.value = airQualityData
                lastAirQualityUpdateTime = currentTime
            } catch (e: Exception) {
                // If there's an error, throw a more descriptive exception
                throw Exception("Failed to refresh weather data: ${e.message}", e)
            }
        }
    }
}
