package bose.ankush.network.di

import bose.ankush.network.api.KtorWeatherApiService
import bose.ankush.network.utils.NetworkConstants
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.repository.WeatherRepository
import bose.ankush.network.repository.WeatherRepositoryImpl
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * Create platform-specific HttpClient
 * This is an expect function that will be implemented differently on each platform
 */
expect fun createPlatformHttpClient(json: Json): HttpClient

/**
 * Factory function to create a WeatherRepository instance
 * This is useful for non-Koin consumers of the network module
 */
fun createWeatherRepository(
    networkConnectivity: NetworkConnectivity,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL
): WeatherRepository {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }
    val httpClient = createPlatformHttpClient(json)
    val apiService = KtorWeatherApiService(httpClient, baseUrl)
    return WeatherRepositoryImpl(apiService, networkConnectivity)
}
