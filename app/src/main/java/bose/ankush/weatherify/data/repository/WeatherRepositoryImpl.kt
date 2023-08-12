package bose.ankush.weatherify.data.repository

import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.data.remote.api.OpenWeatherApiService
import bose.ankush.weatherify.data.remote.dto.AirQualityDto
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.data.remote.dto.WeatherDto
import bose.ankush.weatherify.data.room.WeatherDatabase
import bose.ankush.weatherify.data.room.WeatherEntity
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


    override suspend fun getTodaysWeatherReport(cityName: String): WeatherDto =
        withContext(dispatcher.io) {
            apiService.getTodaysWeatherReport(location = cityName)
        }

    override suspend fun getWeatherForecastList(cityName: String): ForecastDto =
        withContext(dispatcher.io) {
            apiService.getWeatherForecastList(location = cityName)
        }

    override suspend fun getAirQualityReport(lat: String, lang: String): AirQualityDto =
        withContext(dispatcher.io) {
            apiService.getCurrentAirQuality(
                latitude = lat,
                longitude = lang
            )
        }

    override fun getWeatherReport(): Flow<WeatherEntity> =
        weatherDatabase.weatherDao().getWeather()

    /**
     * Method used by view-model when UI sends refresh weather event.
     */
    override suspend fun refreshWeatherData(coordinates: Pair<Double, Double>) {
        withContext(dispatcher.io) {
            try {
                // fetch data from API
                val weatherData = apiService.getOneCallWeather(
                    coordinates.first.toString(),
                    coordinates.second.toString()
                )
                // store the data in room db
                weatherDatabase.weatherDao().refreshWeather(weatherData)
            } catch (e: Exception) {
                // throw exception in case of error
                e.message
            }
        }
    }
}


