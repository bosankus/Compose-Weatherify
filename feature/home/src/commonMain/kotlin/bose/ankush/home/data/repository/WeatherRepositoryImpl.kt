package bose.ankush.home.data.repository

import bose.ankush.home.data.mapper.AirQualityMapper
import bose.ankush.home.data.mapper.NetworkToStorageMapper
import bose.ankush.home.data.mapper.WeatherMapper
import bose.ankush.home.domain.location.HomeGeocoder
import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.domain.repository.WeatherRepository
import bose.ankush.home.domain.repository.WeatherWearSync
import bose.ankush.storage.api.WeatherStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import bose.ankush.network.repository.WeatherRepository as NetworkWeatherRepository

internal class WeatherRepositoryImpl(
    private val networkRepository: NetworkWeatherRepository,
    private val weatherStorage: WeatherStorage,
    private val weatherWearSync: WeatherWearSync,
    private val homeGeocoder: HomeGeocoder,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WeatherRepository {
    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality> =
        weatherStorage.getAirQualityReport(coordinates).map { data ->
            data?.let { AirQualityMapper.mapToDomain(it) } ?: AirQuality()
        }

    override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?> =
        weatherStorage.getWeatherReport(location).map { data -> WeatherMapper.mapToDomain(data) }

    /**
     * Orchestrates data refresh: fetch unified response from network → extract weather + air
     * quality → map → save to storage. Air quality is embedded in the /weather response and may
     * be null for free-tier users — an empty AirQualityData is stored in that case to satisfy the
     * storage contract.
     */
    override suspend fun refreshWeatherData(
        coordinates: Pair<Double, Double>,
        forceRefresh: Boolean,
    ) {
        withContext(ioDispatcher) {
            val lastUpdateTime = weatherStorage.getLastWeatherUpdateTime(coordinates)
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val isDataStale = forceRefresh || (currentTime - lastUpdateTime) > ONE_HOUR_IN_MILLIS

            if (isDataStale) {
                networkRepository.refreshWeatherData(coordinates).fold(
                    onSuccess = {
                        val weatherStorageData =
                            NetworkToStorageMapper.mapWeatherToStorageEntity(it)
                        val airQualityStorageData =
                            NetworkToStorageMapper.mapAirQualityToStorageEntity(it.data?.airQuality)
                        weatherStorage.saveWeatherData(weatherStorageData, airQualityStorageData)
                        weatherStorage.saveLastWeatherUpdateTime(coordinates, currentTime)
                        val locationName =
                            homeGeocoder.reverseGeocode(coordinates.first, coordinates.second)
                                ?: DEFAULT_LOCATION_NAME
                        weatherWearSync.sync(locationName, it)
                    },
                    onFailure = {
                        // Keep serving the last cached snapshot; caller surfaces the flow's error state.
                    },
                )
            }
        }
    }

    override suspend fun clearAllData() {
        withContext(ioDispatcher) {
            weatherStorage.clearAllData()
        }
    }

    companion object {
        private const val ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L
        private const val DEFAULT_LOCATION_NAME = "Current Location"
    }
}
