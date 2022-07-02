package bose.ankush.weatherify.domain.repository

import bose.ankush.weatherify.data.remote.dto.AirQualityDto
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.data.remote.dto.WeatherDto

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherRepository {

    suspend fun getTodaysWeatherReport(cityName: String): WeatherDto

    suspend fun getWeatherForecastList(cityName: String): ForecastDto

    suspend fun getAirQualityReport(lat: String, lang: String): AirQualityDto
}