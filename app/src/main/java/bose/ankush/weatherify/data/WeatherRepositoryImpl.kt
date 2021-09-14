package bose.ankush.weatherify.data

import bose.ankush.weatherify.data.model.CurrentTemperature
import bose.ankush.weatherify.data.model.WeatherForecast
import bose.ankush.weatherify.dispatcher.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dispatcher: DispatcherProvider,
) : WeatherRepository {

    override suspend fun getCurrentTemperature(): CurrentTemperature? =
        withContext(dispatcher.io) { apiService.getCurrentTemperature() }

    override suspend fun getWeatherForecast(): WeatherForecast? =
        withContext(dispatcher.io) { apiService.getWeatherForecast() }

}


