package bose.ankush.storage.impl

import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.room.WeatherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeatherStorageImpl(
    private val weatherDatabase: WeatherDatabase,
) : WeatherStorage {
    // In-memory per-location timestamp map. Keyed by "lat_lon" string.
    // Reset on process restart intentionally — fresh data should be fetched after a cold start.
    private val locationTimestamps = mutableMapOf<String, Long>()

    private fun locationKey(coordinates: Pair<Double, Double>) =
        "${coordinates.first}_${coordinates.second}"

    override fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<Any?> =
        weatherDatabase.weatherDao().getWeather()

    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<Any?> =
        weatherDatabase.weatherDao().getAirQuality()

    override suspend fun getLastWeatherUpdateTime(coordinates: Pair<Double, Double>): Long =
        locationTimestamps[locationKey(coordinates)] ?: 0L

    override suspend fun saveLastWeatherUpdateTime(
        coordinates: Pair<Double, Double>,
        time: Long,
    ) {
        locationTimestamps[locationKey(coordinates)] = time
    }

    override suspend fun saveWeatherData(
        weatherEntity: Any,
        airQualityEntity: Any,
    ) {
        withContext(Dispatchers.IO) {
            if (weatherEntity is WeatherEntity && airQualityEntity is AirQualityEntity) {
                weatherDatabase.weatherDao().refreshWeather(weatherEntity, airQualityEntity)
            }
        }
    }

    override suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            weatherDatabase.weatherDao().clearAll()
        }
        locationTimestamps.clear()
    }
}
