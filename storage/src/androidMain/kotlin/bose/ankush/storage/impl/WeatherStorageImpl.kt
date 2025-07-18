package bose.ankush.storage.impl

import bose.ankush.storage.api.WeatherStorage
import bose.ankush.storage.room.AirQualityEntity
import bose.ankush.storage.room.Weather
import bose.ankush.storage.room.WeatherDatabase
import bose.ankush.storage.room.WeatherEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import bose.ankush.network.model.AirQuality as NetworkAirQuality
import bose.ankush.network.model.WeatherForecast as NetworkWeatherForecast
import bose.ankush.network.repository.WeatherRepository as NetworkWeatherRepository

/**
 * Implementation of WeatherStorage that uses Room database for storage
 * and the network module for fetching data.
 * 
 * This class is responsible for:
 * - Retrieving weather and air quality data from the local database
 * - Refreshing data from the network when needed
 * - Mapping between network models and database entities
 * - Tracking the last update time for weather data
 */
@Singleton
class WeatherStorageImpl @Inject constructor(
    private val networkRepository: NetworkWeatherRepository,
    private val weatherDatabase: WeatherDatabase
) : WeatherStorage {

    /**
     * Gets the latest weather report from the local database
     * @param coordinates Pair of latitude and longitude (not used in current implementation)
     * @return Flow of WeatherEntity as Any?
     */
    override fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<Any?> {
        return weatherDatabase.weatherDao().getWeather()
    }

    /**
     * Gets the latest air quality report from the local database
     * @param coordinates Pair of latitude and longitude (not used in current implementation)
     * @return Flow of AirQualityEntity as Any?
     */
    override fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<Any?> {
        return weatherDatabase.weatherDao().getAirQuality()
    }

    /**
     * Refreshes weather and air quality data from the network and stores it in the local database
     * @param coordinates Pair of latitude and longitude
     * @throws IOException if there's an error refreshing the data
     */
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
            // If there's an error, throw a more specific IOException with detailed information
            throw IOException("Failed to refresh weather data for coordinates (${coordinates.first}, ${coordinates.second}): ${e.message}", e)
        }
    }

    /**
     * Gets the timestamp of the last weather data update
     * @return Timestamp in milliseconds, or 0 if no data is available
     */
    override suspend fun getLastWeatherUpdateTime(): Long {
        val weatherEntity = weatherDatabase.weatherDao().getWeather().firstOrNull()
        return weatherEntity?.lastUpdated ?: 0L
    }

    /**
     * Maps a NetworkWeatherForecast to a WeatherEntity for storage in the database
     * @param weatherData The network model to map
     * @return A WeatherEntity with all fields mapped from the network model
     */
    private fun mapNetworkWeatherToEntity(weatherData: NetworkWeatherForecast): WeatherEntity {
        return WeatherEntity(
            id = 0, // Room will auto-generate this
            lastUpdated = System.currentTimeMillis(),
            alerts = weatherData.alerts?.map { alert ->
                alert?.let {
                    WeatherEntity.Alert(
                        description = it.description,
                        end = it.end,
                        event = it.event,
                        sender_name = it.sender_name,
                        start = it.start
                    )
                }
            },
            current = weatherData.current?.let { current ->
                WeatherEntity.Current(
                    clouds = current.clouds,
                    dt = current.dt,
                    feels_like = current.feels_like,
                    humidity = current.humidity,
                    pressure = current.pressure,
                    sunrise = current.sunrise,
                    sunset = current.sunset,
                    temp = current.temp,
                    uvi = current.uvi,
                    weather = current.weather?.map { weatherCondition ->
                        weatherCondition?.let {
                            Weather(
                                description = it.description,
                                icon = it.icon,
                                id = it.id,
                                main = it.main
                            )
                        }
                    },
                    wind_gust = current.wind_gust,
                    wind_speed = current.wind_speed
                )
            },
            daily = weatherData.daily?.map { daily ->
                daily?.let {
                    WeatherEntity.Daily(
                        clouds = it.clouds,
                        dew_point = it.dew_point,
                        dt = it.dt,
                        humidity = it.humidity,
                        pressure = it.pressure,
                        rain = it.rain,
                        summary = it.summary,
                        sunrise = it.sunrise,
                        sunset = it.sunset,
                        temp = it.temp?.let { temp ->
                            WeatherEntity.Daily.Temp(
                                day = temp.day,
                                eve = temp.eve,
                                max = temp.max,
                                min = temp.min,
                                morn = temp.morn,
                                night = temp.night
                            )
                        },
                        uvi = it.uvi,
                        weather = it.weather?.map { weatherCondition ->
                            weatherCondition?.let {
                                Weather(
                                    description = it.description,
                                    icon = it.icon,
                                    id = it.id,
                                    main = it.main
                                )
                            }
                        },
                        wind_gust = it.wind_gust,
                        wind_speed = it.wind_speed
                    )
                }
            },
            hourly = weatherData.hourly?.map { hourly ->
                hourly?.let { it ->
                    WeatherEntity.Hourly(
                        clouds = it.clouds,
                        dt = it.dt,
                        feels_like = it.feels_like,
                        humidity = it.humidity,
                        temp = it.temp,
                        weather = it.weather?.map { weatherCondition ->
                            weatherCondition?.let {
                                Weather(
                                    description = it.description,
                                    icon = it.icon,
                                    id = it.id,
                                    main = it.main
                                )
                            }
                        }
                    )
                }
            }
        )
    }

    /**
     * Maps a NetworkAirQuality to an AirQualityEntity for storage in the database
     * @param airQualityData The network model to map
     * @return An AirQualityEntity with all fields mapped from the network model
     */
    private fun mapNetworkAirQualityToEntity(airQualityData: NetworkAirQuality): AirQualityEntity {
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
