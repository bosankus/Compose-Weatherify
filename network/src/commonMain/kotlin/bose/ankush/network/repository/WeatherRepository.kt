package bose.ankush.network.repository

import bose.ankush.network.model.WeatherForecast
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun getWeatherReport(coordinates: Pair<Double, Double>): Flow<WeatherForecast?>

    suspend fun refreshWeatherData(coordinates: Pair<Double, Double>)
}
