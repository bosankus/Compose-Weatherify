package bose.ankush.weatherify.data

import bose.ankush.weatherify.dispatcher.DispatcherProvider
import bose.ankush.weatherify.model.CurrentTemperature
import bose.ankush.weatherify.model.WeatherForecast
import kotlinx.coroutines.withContext

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

class WeatherRepositoryImpl(
    private val apiService: ApiService,
    private val dispatcher: DispatcherProvider
) : WeatherRepository {

    override suspend fun getCurrentTemperature(): CurrentTemperature? =
        withContext(dispatcher.io) { apiService.getCurrentTemperature() }

    override suspend fun getWeatherForecast(): WeatherForecast? =
        withContext(dispatcher.io) { apiService.getWeatherForecast() }

}


