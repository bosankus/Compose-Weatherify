package bose.ankush.weatherify.data.repository

import android.content.Context
import androidx.room.withTransaction
import bose.ankush.weatherify.base.common.ConnectivityManager
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.data.mapper.AirQualityMapper
import bose.ankush.weatherify.data.mapper.WeatherMapper
import bose.ankush.weatherify.data.remote.api.OpenWeatherApiService
import bose.ankush.weatherify.data.room.weather.WeatherDatabase
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.WeatherForecast
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.pow

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: OpenWeatherApiService,
    private val weatherDatabase: WeatherDatabase,
    private val dispatcher: DispatcherProvider,
    private val context: Context
) : WeatherRepository {

    companion object {
        // Cache expiration time in milliseconds (30 minutes)
        private const val CACHE_EXPIRATION_TIME = 30 * 60 * 1000L

        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_DELAY = 1000L // 1 second
    }

    /**
     * Helper function to retry a network request with exponential backoff
     * @param block The suspend function to retry
     * @return The result of the suspend function
     * @throws Exception if all retries fail
     */
    private suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = MAX_RETRIES,
        initialDelayMillis: Long = INITIAL_BACKOFF_DELAY,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMillis
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                // If this is the last attempt, throw the exception
                if (attempt == maxRetries - 1) throw e

                // Otherwise, delay and retry
                delay(currentDelay)
                currentDelay = (currentDelay * 2.0.pow(attempt)).toLong()
            }
        }
        // This should never be reached, but is needed for compilation
        throw IllegalStateException("Retry failed after $maxRetries attempts")
    }

    override fun getAirQualityReport(lat: String, lang: String): Flow<AirQuality> =
        weatherDatabase.weatherDao().getAirQuality().map { entity ->
            AirQualityMapper.mapToDomain(entity)
        }

    override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?> =
        weatherDatabase.weatherDao().getWeather().map { entity ->
            WeatherMapper.mapToDomain(entity)
        }

    /**
     * Method used by view-model when UI sends refresh weather event.
     * Checks if the data is stale before fetching new data.
     * Also checks for network connectivity before making API calls.
     */
    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        withContext(dispatcher.io) {
            // Check if data exists in cache
            val currentWeather = weatherDatabase.weatherDao().getWeather().firstOrNull()
            val currentTime = System.currentTimeMillis()

            // Check if network is available
            val isNetworkAvailable = ConnectivityManager.isNetworkAvailable(context)

            // Only fetch new data if:
            // 1. Network is available AND
            // 2. Either data is null OR data is stale (older than cache expiration time)
            if (isNetworkAvailable && 
                (currentWeather == null || (currentTime - currentWeather.lastUpdated > CACHE_EXPIRATION_TIME))) {
                try {
                    // Fetch weather data with retry mechanism
                    val weatherData = retryWithExponentialBackoff {
                        apiService.getOneCallWeather(
                            coordinates.first.toString(),
                            coordinates.second.toString()
                        )
                    }

                    // Fetch air quality data with retry mechanism
                    val airQualityDomain = retryWithExponentialBackoff {
                        apiService.getCurrentAirQuality(
                            latitude = coordinates.first.toString(),
                            longitude = coordinates.second.toString()
                        )
                    }

                    val airQualityEntity = AirQualityMapper.mapToEntity(airQualityDomain)

                    // store the data in room db
                    weatherDatabase.withTransaction {
                        weatherDatabase.weatherDao().refreshWeather(weatherData, airQualityEntity)
                    }
                } catch (e: Exception) {
                    // If there's an error, throw a more descriptive exception
                    throw Exception("Failed to refresh weather data: ${e.message}", e)
                }
            }
        }
    }
}
