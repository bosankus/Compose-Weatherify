package bose.ankush.weatherify.domain.repository

import bose.ankush.weatherify.data.room.weather.WeatherEntity
import bose.ankush.weatherify.domain.model.AirQuality
import kotlinx.coroutines.flow.Flow

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

interface WeatherRepository {

    fun getAirQualityReport(lat: String, lang: String): Flow<AirQuality>

    fun getWeatherReport(location: Pair<Double, Double>): Flow<WeatherEntity?>

    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>)
}