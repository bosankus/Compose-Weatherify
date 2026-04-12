package bose.ankush.weatherify.domain.repository

import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.WeatherForecast
import kotlinx.coroutines.flow.Flow

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherRepository {

    fun getAirQualityReport(coordinates: Pair<Double, Double>): Flow<AirQuality>

    fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherForecast?>

    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>, forceRefresh: Boolean = false)
}
