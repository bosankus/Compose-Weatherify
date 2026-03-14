package bose.ankush.weatherify.data.repository

import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.WeatherEntity as StorageWeatherEntity
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.data.mapper.AirQualityMapper
import bose.ankush.weatherify.data.mapper.WeatherMapper
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.WeatherForecast
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of WeatherRepository that uses the KMM storage module
 * for data access and refresh operations
 */
class WeatherRepositoryImpl @Inject constructor(
    private val weatherStorage: WeatherStorage,
    private val dispatcher: DispatcherProvider
) : WeatherRepository {

    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality> =
        weatherStorage.getAirQualityReport(coordinates).map { entity ->
            (entity as? AirQualityEntity)?.let { AirQualityMapper.mapToDomain(it) } ?: AirQuality()
        }

    override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?> =
        weatherStorage.getWeatherReport(location).map { entity ->
            (entity as? StorageWeatherEntity)?.let { WeatherMapper.mapToDomain(it) }
        }

    /**
     * Method used by view-model when UI sends refresh weather event.
     * Delegates to the storage module for refreshing data.
     * 
     * The app module controls when to refresh based on business rules
     * (e.g., data staleness, user pull-to-refresh)
     */
    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        withContext(dispatcher.io) {
            try {
                // Check if data is stale (older than 1 hour)
                val lastUpdateTime = weatherStorage.getLastWeatherUpdateTime()
                val currentTime = System.currentTimeMillis()
                val isDataStale = (currentTime - lastUpdateTime) > ONE_HOUR_IN_MILLIS

                // Refresh data if it's stale or if this is a forced refresh
                if (isDataStale) {
                    weatherStorage.refreshWeatherData(coordinates)
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
