package bose.ankush.weatherify.model.repository

import bose.ankush.weatherify.model.model.AvgForecast
import bose.ankush.weatherify.model.model.CurrentTemperature
import bose.ankush.weatherify.util.ResultData
import bose.ankush.weatherify.util.UiText
import kotlinx.coroutines.flow.Flow

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherRepository {

    fun getCurrentTemperature(): Flow<ResultData<CurrentTemperature>>

    fun getWeatherForecast(): Flow<ResultData<List<AvgForecast>>>

    fun errorResponse(errorCode: Int): UiText.StringResource
}