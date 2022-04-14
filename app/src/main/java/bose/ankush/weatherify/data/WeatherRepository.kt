package bose.ankush.weatherify.data

import bose.ankush.weatherify.data.model.AvgForecast
import bose.ankush.weatherify.data.model.CurrentTemperature
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