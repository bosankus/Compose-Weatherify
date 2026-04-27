package bose.ankush.network.api

import bose.ankush.network.model.WeatherForecast
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Ktor implementation of WeatherApiService
 */
class KtorWeatherApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : WeatherApiService {

    override suspend fun getOneCallWeather(
        latitude: String,
        longitude: String
    ): WeatherForecast {
        return httpClient.get("$baseUrl/weather") {
            parameter("lat", latitude)
            parameter("lon", longitude)
        }.body()
    }
}
