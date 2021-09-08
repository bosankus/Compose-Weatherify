package bose.ankush.weatherify.data

import bose.ankush.weatherify.data.model.CurrentTemperature
import bose.ankush.weatherify.data.model.WeatherForecast

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherRepository {

    suspend fun getCurrentTemperature(): CurrentTemperature?

    suspend fun getWeatherForecast(): WeatherForecast?
}