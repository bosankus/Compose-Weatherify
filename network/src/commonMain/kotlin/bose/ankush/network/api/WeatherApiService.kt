package bose.ankush.network.api

import bose.ankush.network.model.WeatherForecast

interface WeatherApiService {
    suspend fun getOneCallWeather(
        latitude: String,
        longitude: String,
    ): WeatherForecast
}
