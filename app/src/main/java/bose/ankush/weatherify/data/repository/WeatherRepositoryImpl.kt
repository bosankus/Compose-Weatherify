package bose.ankush.weatherify.data.repository

import bose.ankush.weatherify.data.remote.ApiService
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.data.remote.dto.WeatherDto
import bose.ankush.weatherify.dispatcher.DispatcherProvider
import bose.ankush.weatherify.domain.repository.WeatherRepository
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


    override suspend fun getTodaysWeatherReport(): WeatherDto =
        withContext(dispatcher.io) { apiService.getTodaysWeatherReport() }

    override suspend fun getWeatherForecastList(): ForecastDto =
        withContext(dispatcher.io) { apiService.getWeatherForecastList() }

}


