package bose.ankush.weatherify.domain.repository

import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.domain.model.Weather
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.data.remote.dto.WeatherDto
import kotlinx.coroutines.flow.Flow

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherRepository {

    suspend fun getTodaysWeatherReport(): WeatherDto

    suspend fun getWeatherForecastList(): ForecastDto

    fun errorResponse(errorCode: Int): UiText.StringResource
}