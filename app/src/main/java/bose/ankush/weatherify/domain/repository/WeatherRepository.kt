package bose.ankush.weatherify.domain.repository

import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.domain.model.Weather
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import kotlinx.coroutines.flow.Flow

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherRepository {

    fun getCurrentTemperature(): Flow<ResultData<Weather>>

    fun getWeatherForecast(): Flow<ResultData<List<AvgForecast>>>

    suspend fun getWeatherForecastList(): List<ForecastDto.ForecastList>

    fun errorResponse(errorCode: Int): UiText.StringResource
}