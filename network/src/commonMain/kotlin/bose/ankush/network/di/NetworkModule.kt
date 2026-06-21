package bose.ankush.network.di

import bose.ankush.network.api.KtorFeedbackApiService
import bose.ankush.network.api.KtorLocationApiService
import bose.ankush.network.api.KtorPaymentApiService
import bose.ankush.network.api.KtorServiceApiService
import bose.ankush.network.api.KtorWeatherApiService
import bose.ankush.network.api.PaymentApiService
import bose.ankush.network.auth.api.KtorAuthApiService
import bose.ankush.network.auth.interceptor.configureAuth
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.repository.AuthRepositoryImpl
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.repository.FeedbackRepository
import bose.ankush.network.repository.FeedbackRepositoryImpl
import bose.ankush.network.repository.LocationRepository
import bose.ankush.network.repository.LocationRepositoryImpl
import bose.ankush.network.repository.ServiceRepository
import bose.ankush.network.repository.ServiceRepositoryImpl
import bose.ankush.network.repository.WeatherRepository
import bose.ankush.network.repository.WeatherRepositoryImpl
import bose.ankush.network.utils.NetworkConstants
import bose.ankush.storage.api.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(json: Json): HttpClient

@Suppress("unused")
fun createBasicHttpClient(): HttpClient {
    val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
            coerceInputValues = true
        }
    val client = createPlatformHttpClient(json)
    return client.config {
        install(ContentNegotiation) {
            json(json)
        }
    }
}

fun createTokenManager(
    tokenStorage: TokenStorage,
    authRepository: AuthRepository,
): TokenManager = TokenManager(tokenStorage, authRepository)

fun createWeatherRepository(
    networkConnectivity: NetworkConnectivity,
    tokenStorage: TokenStorage,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL,
): WeatherRepository {
    val authRepository = createAuthRepository(tokenStorage, baseUrl)
    val tokenManager = createTokenManager(tokenStorage, authRepository)
    val httpClient = createAuthenticatedHttpClient(tokenManager)
    val apiService = KtorWeatherApiService(httpClient, baseUrl)
    return WeatherRepositoryImpl(apiService, networkConnectivity)
}

fun createPaymentApiService(
    tokenStorage: TokenStorage,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL,
): PaymentApiService {
    val authRepository = createAuthRepository(tokenStorage, baseUrl)
    val tokenManager = createTokenManager(tokenStorage, authRepository)
    val httpClient = createAuthenticatedHttpClient(tokenManager)
    return KtorPaymentApiService(httpClient, baseUrl)
}

fun createAuthenticatedHttpClient(tokenManager: TokenManager): HttpClient {
    val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
            coerceInputValues = true
        }

    val client = createPlatformHttpClient(json)
    return client.config {
        install(ContentNegotiation) {
            json(json)
        }
        configureAuth(tokenManager)
        // SECURITY: Use LogLevel.NONE in production to prevent JWT token exposure in logs
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
    }
}

/**
 * Legacy function for backward compatibility
 * Creates an HttpClient with basic authentication configuration (no token refresh)
 */
fun createAuthenticatedHttpClient(tokenStorage: TokenStorage): HttpClient {
    val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
            coerceInputValues = true
        }

    val client = createPlatformHttpClient(json)
    return client.config {
        install(ContentNegotiation) {
            json(json)
        }
        configureAuth(tokenStorage)
        // SECURITY: Use LogLevel.NONE in production to prevent JWT token exposure in logs
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
    }
}

fun createAuthRepository(
    tokenStorage: TokenStorage,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL,
): AuthRepository {
    // Use the legacy HttpClient for AuthRepository to avoid circular dependency
    val httpClient = createAuthenticatedHttpClient(tokenStorage)
    val apiService = KtorAuthApiService(httpClient, baseUrl)
    return AuthRepositoryImpl(apiService, tokenStorage)
}

fun createLocationRepository(
    tokenStorage: TokenStorage,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL,
): LocationRepository {
    val authRepository = createAuthRepository(tokenStorage, baseUrl)
    val tokenManager = createTokenManager(tokenStorage, authRepository)
    val httpClient = createAuthenticatedHttpClient(tokenManager)
    val apiService = KtorLocationApiService(httpClient, baseUrl)
    return LocationRepositoryImpl(apiService)
}

fun createFeedbackRepository(
    networkConnectivity: NetworkConnectivity,
    tokenStorage: TokenStorage,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL,
): FeedbackRepository {
    val authRepository = createAuthRepository(tokenStorage, baseUrl)
    val tokenManager = createTokenManager(tokenStorage, authRepository)
    val httpClient = createAuthenticatedHttpClient(tokenManager)
    val apiService = KtorFeedbackApiService(httpClient, baseUrl)
    return FeedbackRepositoryImpl(apiService, networkConnectivity)
}

fun createServiceRepository(baseUrl: String = NetworkConstants.WEATHER_BASE_URL): ServiceRepository {
    val httpClient = createBasicHttpClient()
    val apiService = KtorServiceApiService(httpClient, baseUrl)
    return ServiceRepositoryImpl(apiService)
}
