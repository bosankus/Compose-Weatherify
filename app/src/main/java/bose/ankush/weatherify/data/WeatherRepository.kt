package bose.ankush.weatherify.data

import bose.ankush.weatherify.model.CurrentTemperature
import bose.ankush.weatherify.model.WeatherForecast

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

class WeatherRepository(private val apiService: ApiService) : WeatherImpl {

    override suspend fun getCurrentTemperature(): CurrentTemperature? =
        apiService.getCurrentTemperature()

    override suspend fun getWeatherForecast(): WeatherForecast? =
        apiService.getWeatherForecast()

}


