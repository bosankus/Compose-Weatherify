package bose.ankush.weatherify.data.repository

import bose.ankush.weatherify.data.remote.api.OpenWeatherApiService
import bose.ankush.weatherify.data.remote.dto.AirQualityDto
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.data.remote.dto.WeatherDto
import bose.ankush.weatherify.dispatcher.DispatcherProvider
import bose.ankush.weatherify.domain.repository.WeatherRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

class WeatherRepositoryImpl @Inject constructor(
    private val openWeatherApiService: OpenWeatherApiService,
    private val dispatcher: DispatcherProvider,
) : WeatherRepository {


    override suspend fun getTodaysWeatherReport(cityName: String): WeatherDto =
        withContext(dispatcher.io) { openWeatherApiService.getTodaysWeatherReport(location = cityName) }

    override suspend fun getWeatherForecastList(cityName: String): ForecastDto =
        withContext(dispatcher.io) { openWeatherApiService.getWeatherForecastList(location = cityName) }

    // TODO: Implementation is dependent on lat,lang
    override suspend fun getAirQualityReport(lat: String, lang: String): AirQualityDto =
        withContext(dispatcher.io) {
            openWeatherApiService.getCurrentAirQuality(latitude = lat, longitude = lang)
        }
}


