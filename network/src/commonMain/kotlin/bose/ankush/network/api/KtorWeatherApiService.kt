package bose.ankush.network.api

import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.WeatherForecast
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorWeatherApiService(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager,
    private val baseUrl: String,
) : WeatherApiService {
    override suspend fun getOneCallWeather(
        latitude: String,
        longitude: String,
    ): WeatherForecast =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                get("$baseUrl/weather") {
                    parameter("lat", latitude)
                    parameter("lon", longitude)
                    authConfig()
                }
            }.body()
}
