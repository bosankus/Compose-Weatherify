package bose.ankush.storage.api

import kotlinx.coroutines.flow.Flow

/**
 * Interface for weather data storage operations.
 *
 * This interface defines the contract for storing and retrieving weather and air quality data.
 * It abstracts the underlying storage mechanism (e.g., Room database) from the rest of the application.
 * Implementations of this interface are responsible for:
 * - Retrieving weather and air quality data
 * - Refreshing data from the network
 * - Tracking the last update time
 */
interface WeatherStorage {
    /**
     * Get weather forecast data for a location
     * @param coordinates Pair of latitude and longitude
     * @return Flow of weather forecast data
     */
    fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<Any?>

    /**
     * Get air quality data for a location
     * @param coordinates Pair of latitude and longitude
     * @return Flow of air quality data
     */
    fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<Any?>

    /**
     * Get the timestamp of the last weather data update
     * @return Timestamp in milliseconds
     */
    suspend fun getLastWeatherUpdateTime(): Long

    /**
     * Save weather and air quality data to storage.
     *
     * This method is called by the orchestration layer after fetching and mapping data from network.
     *
     * @param weatherEntity The weather data to save
     * @param airQualityEntity The air quality data to save
     */
    suspend fun saveWeatherData(weatherEntity: Any, airQualityEntity: Any)
}
