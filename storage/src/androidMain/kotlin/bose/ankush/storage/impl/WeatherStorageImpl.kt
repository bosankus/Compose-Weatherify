package bose.ankush.storage.impl

import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.room.WeatherEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Implementation of WeatherStorage that uses Room database for persistence.
 *
 * This class is responsible ONLY for:
 * - Reading weather data from the local database
 * - Reading air quality data from the local database
 * - Saving weather data to the database (called by orchestration layer)
 *
 * Data synchronization (fetch from network, map, save to DB) is handled
 * by the orchestration layer (WeatherRepository in app module).
 */
class WeatherStorageImpl(
    private val weatherDatabase: WeatherDatabase
) : WeatherStorage {

    override fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<Any?> {
        return weatherDatabase.weatherDao().getWeather()
    }

    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<Any?> {
        return weatherDatabase.weatherDao().getAirQuality()
    }

    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        // This is handled by the orchestration layer (WeatherRepository)
        // Storage should not be responsible for fetching or syncing data
        throw UnsupportedOperationException(
            "Use WeatherRepository from app layer to refresh data. " +
            "Storage module only handles persistence."
        )
    }

    override suspend fun getLastWeatherUpdateTime(): Long {
        val weatherEntity = weatherDatabase.weatherDao().getWeather().firstOrNull()
        return weatherEntity?.lastUpdated ?: 0L
    }

    /**
     * Save weather and air quality data to the database.
     * Called by the orchestration layer (WeatherRepository in app module) after fetching and mapping from network.
     *
     * This is public because it's called by the orchestration layer in the app module.
     *
     * @param weatherEntity The weather data to save
     * @param airQualityEntity The air quality data to save
     */
    fun saveWeatherData(
        weatherEntity: WeatherEntity,
        airQualityEntity: AirQualityEntity
    ) {
        weatherDatabase.weatherDao().refreshWeather(weatherEntity, airQualityEntity)
    }
}
