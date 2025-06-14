package bose.ankush.network.di

import bose.ankush.network.api.KtorWeatherApiService
import bose.ankush.network.api.WeatherApiService
import bose.ankush.network.common.Constants
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.repository.WeatherRepository
import bose.ankush.network.repository.WeatherRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for network dependencies
 */
fun networkModule(baseUrl: String = Constants.WEATHER_BASE_URL): Module = module {
    // JSON configuration
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
        }
    }

    // HttpClient with platform-specific engine
    single {
        createPlatformHttpClient(get())
    }

    // API Service
    single<WeatherApiService> {
        KtorWeatherApiService(get(), baseUrl)
    }

    // Repository
    single<WeatherRepository> {
        WeatherRepositoryImpl(get(), get())
    }
}

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
    baseUrl: String = Constants.WEATHER_BASE_URL
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
