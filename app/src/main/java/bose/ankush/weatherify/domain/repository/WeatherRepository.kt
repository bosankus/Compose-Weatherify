package bose.ankush.weatherify.domain.repository

import bose.ankush.weatherify.data.remote.dto.AirQualityDto
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.data.remote.dto.WeatherDto
import bose.ankush.weatherify.data.room.WeatherEntity
import kotlinx.coroutines.flow.Flow

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherRepository {

    suspend fun getTodaysWeatherReport(cityName: String): WeatherDto

    suspend fun getWeatherForecastList(cityName: String): ForecastDto

    suspend fun getAirQualityReport(lat: String, lang: String): AirQualityDto

    fun getWeatherReport(): Flow<WeatherEntity>

    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>)
}