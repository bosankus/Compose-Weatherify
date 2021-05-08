package bose.ankush.weatherify.data

import bose.ankush.weatherify.model.CurrentTemperature
import bose.ankush.weatherify.model.WeatherForecast
import bose.ankush.weatherify.util.ResultData

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherImpl {

    suspend fun getCurrentTemperature(): CurrentTemperature?

    suspend fun getWeatherForecast(): WeatherForecast?
}