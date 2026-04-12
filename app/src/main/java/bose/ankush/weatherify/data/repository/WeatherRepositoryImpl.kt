package bose.ankush.weatherify.data.repository

import bose.ankush.network.repository.WeatherRepository as NetworkWeatherRepository
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.impl.WeatherStorageImpl
import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.WeatherEntity
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.data.mapper.AirQualityMapper
import bose.ankush.weatherify.data.mapper.NetworkToStorageMapper
import bose.ankush.weatherify.data.mapper.WeatherMapper
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.WeatherForecast
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Domain-layer repository that orchestrates between network and storage modules.
 *
 * Responsibilities:
 * - Fetch weather data from network (NetworkWeatherRepository)
 * - Map network models to storage entities
 * - Save to local storage (WeatherStorage)
 * - Provide domain models to UI layer (via mappers)
 * - Handle business logic (data staleness checking)
 * - Manage IO operations via DispatcherProvider
 *
 * This is the single source of truth for weather data operations in the app.
 */
class WeatherRepositoryImpl @Inject constructor(
    private val networkRepository: NetworkWeatherRepository,
    private val weatherStorage: WeatherStorage,
    private val dispatcher: DispatcherProvider
) : WeatherRepository {

    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality> =
        weatherStorage.getAirQualityReport(coordinates).map { entity ->
            (entity as? AirQualityEntity)?.let { AirQualityMapper.mapToDomain(it) } ?: AirQuality()
        }

    override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?> =
        weatherStorage.getWeatherReport(location).map { entity ->
            (entity as? WeatherEntity)?.let { WeatherMapper.mapToDomain(it) }
        }

    /**
     * Orchestrates data refresh: fetch from network → map → save to storage
     *
     * This method handles:
     * 1. Staleness checking (data older than 1 hour)
     * 2. Network fetching (delegates to NetworkWeatherRepository)
     * 3. Model mapping (network → storage entities)
     * 4. Persisting (saves to local storage)
     *
     * The app module controls when to refresh based on business rules.
     */
    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>, forceRefresh: Boolean) {
        withContext(dispatcher.io) {
            try {
                // Step 1: Check if data is stale (older than 1 hour)
                val lastUpdateTime = weatherStorage.getLastWeatherUpdateTime()
                val currentTime = System.currentTimeMillis()
                val isDataStale = forceRefresh || (currentTime - lastUpdateTime) > ONE_HOUR_IN_MILLIS

                // Step 2: Refresh data if it's stale or forced
                if (isDataStale) {
                    // Fetch from network
                    networkRepository.refreshWeatherData(coordinates)

                    // Get the fetched data from network repository
                    val weatherData = networkRepository.getWeatherReport(coordinates).firstOrNull()
                    val airQualityData = networkRepository.getAirQualityReport(coordinates).firstOrNull()

                    if (weatherData != null && airQualityData != null) {
                        // Step 3: Map network models to storage entities using mapper
                        val weatherEntity = NetworkToStorageMapper.mapWeatherToStorageEntity(weatherData)
                        val airQualityEntity = NetworkToStorageMapper.mapAirQualityToStorageEntity(airQualityData)

                        // Step 4: Save to storage
                        (weatherStorage as WeatherStorageImpl).saveWeatherData(weatherEntity, airQualityEntity)
                    }
                }
            } catch (e: Exception) {
                // If there's an error, throw a more descriptive exception
                throw Exception("Failed to refresh weather data: ${e.message}", e)
            }
        }
    }


    companion object {
        private const val ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L
    }
}
