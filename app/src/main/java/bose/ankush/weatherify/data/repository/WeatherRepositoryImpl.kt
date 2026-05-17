package bose.ankush.weatherify.data.repository

import bose.ankush.storage.api.WeatherStorage
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
import bose.ankush.network.repository.WeatherRepository as NetworkWeatherRepository

class WeatherRepositoryImpl
@Inject
constructor(
    private val networkRepository: NetworkWeatherRepository,
    private val weatherStorage: WeatherStorage,
    private val dispatcher: DispatcherProvider,
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
     * Orchestrates data refresh: fetch unified response from network → extract weather + air
     * quality → map → save to storage.
     *
     * Air quality is now embedded in the /weather response and may be null for free-tier users.
     * In that case an empty AirQualityEntity is stored to satisfy the storage contract.
     */
    override suspend fun refreshWeatherData(
        coordinates: Pair<Double, Double>,
        forceRefresh: Boolean,
    ) {
        withContext(dispatcher.io) {
            val lastUpdateTime = weatherStorage.getLastWeatherUpdateTime(coordinates)
            val currentTime = System.currentTimeMillis()
            val isDataStale = forceRefresh || (currentTime - lastUpdateTime) > ONE_HOUR_IN_MILLIS

            if (isDataStale) {
                networkRepository.refreshWeatherData(coordinates)

                val weatherData = networkRepository.getWeatherReport(coordinates).firstOrNull()

                if (weatherData != null) {
                    val weatherEntity =
                        NetworkToStorageMapper.mapWeatherToStorageEntity(weatherData)
                    val airQualityEntity =
                        NetworkToStorageMapper.mapAirQualityToStorageEntity(
                            weatherData.data?.airQuality,
                        )
                    weatherStorage.saveWeatherData(weatherEntity, airQualityEntity)
                    weatherStorage.saveLastWeatherUpdateTime(coordinates, currentTime)
                }
            }
        }
    }

    override suspend fun clearAllData() {
        withContext(dispatcher.io) {
            weatherStorage.clearAllData()
        }
    }

    companion object {
        private const val ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L
    }
}
