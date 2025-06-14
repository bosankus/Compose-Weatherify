package bose.ankush.weatherify.data.repository

import androidx.room.withTransaction
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.data.mapper.AirQualityMapper
import bose.ankush.weatherify.data.mapper.NetworkMapper
import bose.ankush.weatherify.data.mapper.WeatherMapper
import bose.ankush.weatherify.data.room.weather.WeatherDatabase
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.WeatherForecast
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of WeatherRepository that uses the KMM network module
 * and stores data in the local database
 */
class WeatherRepositoryImpl @Inject constructor(
    private val networkRepository: bose.ankush.network.repository.WeatherRepository,
    private val weatherDatabase: WeatherDatabase,
    private val dispatcher: DispatcherProvider
) : WeatherRepository {

    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality> =
        weatherDatabase.weatherDao().getAirQuality().map { entity ->
            AirQualityMapper.mapToDomain(entity)
        }

    override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?> =
        weatherDatabase.weatherDao().getWeather().map { entity ->
            WeatherMapper.mapToDomain(entity)
        }

    /**
     * Method used by view-model when UI sends refresh weather event.
     * Delegates to the network module's repository for fetching data,
     * then stores it in the local database.
     */
    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        withContext(dispatcher.io) {
            try {
                // Delegate to the network module's repository to refresh data
                networkRepository.refreshWeatherData(coordinates)

                // Get the latest data from the network repository
                val weatherData = networkRepository.getWeatherReport(coordinates).firstOrNull()
                val airQualityData = networkRepository.getAirQualityReport(coordinates).firstOrNull()

                if (weatherData != null && airQualityData != null) {
                    // Convert network models to app models using the NetworkMapper
                    val weatherEntity = NetworkMapper.mapWeatherForecastToEntity(weatherData)
                    val airQualityEntity = NetworkMapper.mapAirQualityToEntity(airQualityData)

                    // Store the data in room db
                    weatherDatabase.withTransaction {
                        weatherDatabase.weatherDao().refreshWeather(weatherEntity, airQualityEntity)
                    }
                }
            } catch (e: Exception) {
                // If there's an error, throw a more descriptive exception
                throw Exception("Failed to refresh weather data: ${e.message}", e)
            }
        }
    }
}
