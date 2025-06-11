package bose.ankush.weatherify.data.repository

import androidx.room.withTransaction
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.data.remote.api.OpenWeatherApiService
import bose.ankush.weatherify.data.room.weather.WeatherDatabase
import bose.ankush.weatherify.data.room.weather.WeatherEntity
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: OpenWeatherApiService,
    private val weatherDatabase: WeatherDatabase,
    private val dispatcher: DispatcherProvider,
) : WeatherRepository {

    override fun getAirQualityReport(lat: String, lang: String): Flow<AirQuality> =
        weatherDatabase.weatherDao().getAirQuality()

    override fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherEntity?> =
        weatherDatabase.weatherDao().getWeather()

    /**
     * Method used by view-model when UI sends refresh weather event.
     */
    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        withContext(dispatcher.io) {
            val weatherData = apiService.getOneCallWeather(
                coordinates.first.toString(),
                coordinates.second.toString()
            )
            val airQuality = apiService.getCurrentAirQuality(
                latitude = coordinates.first.toString(),
                longitude = coordinates.second.toString()
            )
            // store the data in room db
            weatherDatabase.withTransaction {
                weatherDatabase.weatherDao().refreshWeather(weatherData, airQuality)
            }
        }
    }
}


