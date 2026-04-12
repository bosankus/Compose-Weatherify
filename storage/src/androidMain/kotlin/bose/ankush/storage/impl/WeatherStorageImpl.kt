package bose.ankush.storage.impl

import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.room.WeatherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

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


    override suspend fun getLastWeatherUpdateTime(): Long {
        val weatherEntity = weatherDatabase.weatherDao().getWeather().firstOrNull()
        return weatherEntity?.lastUpdated ?: 0L
    }

    override suspend fun saveWeatherData(weatherEntity: Any, airQualityEntity: Any) {
        withContext(Dispatchers.IO) {
            if (weatherEntity is WeatherEntity && airQualityEntity is AirQualityEntity) {
                weatherDatabase.weatherDao().refreshWeather(weatherEntity, airQualityEntity)
            }
        }
    }
}
