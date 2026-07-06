package bose.ankush.storage

import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.model.AirQualityData
import bose.ankush.storage.model.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory only — there is no iOS Room/SQLite actual in this repo yet (Room KMP-on-iOS is a
 * separate follow-up spanning the bundled SQLite driver + moving `@Entity`/`@Dao` to commonMain).
 * Data does not survive process death, unlike the Android Room-backed implementation.
 */
class WeatherStorageImpl : WeatherStorage {
    private val weather = MutableStateFlow<WeatherData?>(null)
    private val airQuality = MutableStateFlow<AirQualityData?>(null)
    private val lastUpdateTimes = mutableMapOf<String, Long>()

    private fun locationKey(coordinates: Pair<Double, Double>) = "${coordinates.first}_${coordinates.second}"

    override fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<WeatherData?> = weather

    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQualityData?> = airQuality

    override suspend fun getLastWeatherUpdateTime(coordinates: Pair<Double, Double>): Long =
        lastUpdateTimes[locationKey(coordinates)] ?: 0L

    override suspend fun saveLastWeatherUpdateTime(
        coordinates: Pair<Double, Double>,
        time: Long,
    ) {
        lastUpdateTimes[locationKey(coordinates)] = time
    }

    override suspend fun saveWeatherData(
        weatherData: WeatherData,
        airQualityData: AirQualityData,
    ) {
        weather.value = weatherData
        airQuality.value = airQualityData
    }

    override suspend fun clearAllData() {
        weather.value = null
        airQuality.value = null
        lastUpdateTimes.clear()
    }
}
