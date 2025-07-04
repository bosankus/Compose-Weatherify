package bose.ankush.storage.api

import kotlinx.coroutines.flow.Flow

/**
 * Interface for weather data storage operations
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
     * Refresh weather data from the network and store it
     * @param coordinates Pair of latitude and longitude
     */
    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>)

    /**
     * Get the timestamp of the last weather data update
     * @return Timestamp in milliseconds
     */
    suspend fun getLastWeatherUpdateTime(): Long
}
