package bose.ankush.weatherify.domain.repository

import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.data.remote.dto.WeatherDto

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherRepository {

    suspend fun getTodaysWeatherReport(): WeatherDto

    suspend fun getWeatherForecastList(): ForecastDto
}