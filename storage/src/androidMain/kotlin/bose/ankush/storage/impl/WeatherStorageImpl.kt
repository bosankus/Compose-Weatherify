package bose.ankush.storage.impl

import bose.ankush.network.model.AirQuality as NetworkAirQuality
import bose.ankush.network.model.WeatherForecast as NetworkWeatherForecast
import bose.ankush.network.repository.WeatherRepository as NetworkWeatherRepository
import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.room.WeatherEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of WeatherStorage that uses Room database for storage
 * and the network module for fetching data
 */
@Singleton
class WeatherStorageImpl @Inject constructor(
    private val networkRepository: NetworkWeatherRepository,
    private val weatherDatabase: WeatherDatabase
) : WeatherStorage {

    override fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<Any?> {
        return weatherDatabase.weatherDao().getWeather().map { entity ->
            // Return the entity as is - the app module will handle mapping to domain model
            entity
        }
    }

    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<Any?> {
        return weatherDatabase.weatherDao().getAirQuality().map { entity ->
            // Return the entity as is - the app module will handle mapping to domain model
            entity
        }
    }

    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        try {
            // Delegate to the network module's repository to refresh data
            networkRepository.refreshWeatherData(coordinates)

            // Get the latest data from the network repository
            val weatherData = networkRepository.getWeatherReport(coordinates).firstOrNull()
            val airQualityData = networkRepository.getAirQualityReport(coordinates).firstOrNull()

            if (weatherData != null && airQualityData != null) {
                // Convert network models to storage entities
                val weatherEntity = mapNetworkWeatherToEntity(weatherData)
                val airQualityEntity = mapNetworkAirQualityToEntity(airQualityData)

                // Store the data in room db
                weatherDatabase.weatherDao().refreshWeather(weatherEntity, airQualityEntity)
            }
        } catch (e: Exception) {
            // If there's an error, throw a more descriptive exception
            throw Exception("Failed to refresh weather data: ${e.message}", e)
        }
    }

    override suspend fun getLastWeatherUpdateTime(): Long {
        val weatherEntity = weatherDatabase.weatherDao().getWeather().firstOrNull()
        return weatherEntity?.lastUpdated ?: 0L
    }

    // Helper methods to map network models to storage entities
    private fun mapNetworkWeatherToEntity(weatherData: NetworkWeatherForecast): WeatherEntity {
        // This is a simplified implementation - a real implementation would map all fields
        return WeatherEntity(
            id = 0, // Room will auto-generate this
            lastUpdated = System.currentTimeMillis(),
            // Other fields would be mapped here
            alerts = null,
            current = null,
            daily = null,
            hourly = null
        )
    }

    private fun mapNetworkAirQualityToEntity(airQualityData: NetworkAirQuality): AirQualityEntity {
        // This is a simplified implementation - a real implementation would map all fields
        return AirQualityEntity(
            id = null, // Room will auto-generate this
            aqi = airQualityData.aqi,
            co = airQualityData.co,
            no2 = airQualityData.no2,
            o3 = airQualityData.o3,
            so2 = airQualityData.so2,
            pm10 = airQualityData.pm10,
            pm25 = airQualityData.pm25
        )
    }
}
