package bose.ankush.network.di

import bose.ankush.network.api.FeedbackApiService
import bose.ankush.network.api.KtorFeedbackApiService
import bose.ankush.network.api.KtorPaymentApiService
import bose.ankush.network.api.KtorWeatherApiService
import bose.ankush.network.api.PaymentApiService
import bose.ankush.network.auth.api.KtorAuthApiService
import bose.ankush.network.auth.interceptor.configureAuth
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.repository.AuthRepositoryImpl
import bose.ankush.storage.api.TokenStorage
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.repository.FeedbackRepository
import bose.ankush.network.repository.FeedbackRepositoryImpl
import bose.ankush.network.repository.WeatherRepository
import bose.ankush.network.repository.WeatherRepositoryImpl
import bose.ankush.network.utils.NetworkConstants
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Create platform-specific HttpClient
 * This is an expect function that will be implemented differently on each platform
 */
expect fun createPlatformHttpClient(json: Json): HttpClient

/**
 * Creates a basic HttpClient without auth.
 */
@Suppress("unused")
fun createBasicHttpClient(): HttpClient {
    val json = Json {
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

/**
 * Creates a TokenManager instance
 * @param tokenStorage The storage for authentication tokens
 * @param authRepository The repository for authentication operations
 * @return A TokenManager instance
 */
fun createTokenManager(
    tokenStorage: TokenStorage,
    authRepository: AuthRepository
): TokenManager {
    return TokenManager(tokenStorage, authRepository)
}

/**
 * Factory function to create a WeatherRepository instance
 * This is useful for non-Koin consumers of the network module
 *
 * @param networkConnectivity The network connectivity checker
 * @param tokenStorage The storage for authentication tokens (required for JWT authentication)
 * @param baseUrl The base URL for API requests
 * @return A WeatherRepository instance with authentication
 */
fun createWeatherRepository(
    networkConnectivity: NetworkConnectivity,
    tokenStorage: TokenStorage,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL
): WeatherRepository {
    // Create AuthRepository first (needed for TokenManager)
    val authRepository = createAuthRepository(tokenStorage, baseUrl)

    // Create TokenManager
    val tokenManager = createTokenManager(tokenStorage, authRepository)

    // Create an authenticated HttpClient that will include the JWT token in requests
    val httpClient = createAuthenticatedHttpClient(tokenManager)
    val apiService = KtorWeatherApiService(httpClient, baseUrl)
    return WeatherRepositoryImpl(apiService, networkConnectivity)
}

/**
 * Factory function to create a [PaymentApiService] with an authenticated HTTP client.
 * Consumed by the feature-payment module's Koin DI setup in the host application.
 */
fun createPaymentApiService(
    tokenStorage: TokenStorage,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL,
): PaymentApiService {
    val authRepository = createAuthRepository(tokenStorage, baseUrl)
    val tokenManager = createTokenManager(tokenStorage, authRepository)
    val httpClient = createAuthenticatedHttpClient(tokenManager)
    return KtorPaymentApiService(httpClient, baseUrl)
}

/**
 * Creates an HttpClient with authentication configuration using TokenManager
 * @param tokenManager The manager for JWT tokens
 * @return An HttpClient configured with authentication and token refresh
 */
fun createAuthenticatedHttpClient(tokenManager: TokenManager): HttpClient {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
        coerceInputValues = true
    }

    // Create a platform-specific HttpClient with authentication configuration
    val client = createPlatformHttpClient(json)
    return client.config {
        // Install ContentNegotiation plugin
        install(ContentNegotiation) {
            json(json)
        }

        // Add authentication configuration with token refresh
        configureAuth(tokenManager)

        // Install Logging plugin - SECURITY: Use LogLevel.NONE in production to prevent JWT token exposure in logs
        // Debug mode can be enabled per-platform in androidMain/iosMain if needed
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
    }
}

/**
 * Legacy function for backward compatibility
 * Creates an HttpClient with basic authentication configuration
 * @param tokenStorage The storage for authentication tokens
 * @return An HttpClient configured with authentication (no token refresh)
 */
fun createAuthenticatedHttpClient(tokenStorage: TokenStorage): HttpClient {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
        coerceInputValues = true
    }

    // Create a platform-specific HttpClient with authentication configuration
    val client = createPlatformHttpClient(json)
    return client.config {
        // Install ContentNegotiation plugin
        install(ContentNegotiation) {
            json(json)
        }

        // Add authentication configuration
        configureAuth(tokenStorage)

        // Install Logging plugin - SECURITY: Use LogLevel.NONE in production to prevent JWT token exposure in logs
        // Debug mode can be enabled per-platform in androidMain/iosMain if needed
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
    }
}

/**
 * Factory function to create an AuthRepository instance
 * This is useful for non-Koin consumers of the network module
 */
fun createAuthRepository(
    tokenStorage: TokenStorage,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL
): AuthRepository {
    // Use the legacy HttpClient for AuthRepository to avoid circular dependency
    val httpClient = createAuthenticatedHttpClient(tokenStorage)
    val apiService = KtorAuthApiService(httpClient, baseUrl)
    return AuthRepositoryImpl(apiService, tokenStorage)
}


/**
 * Factory function to create a FeedbackRepository instance
 */
fun createFeedbackRepository(
    networkConnectivity: NetworkConnectivity,
    tokenStorage: TokenStorage,
    baseUrl: String = NetworkConstants.WEATHER_BASE_URL
): FeedbackRepository {
    val authRepository = createAuthRepository(tokenStorage, baseUrl)
    val tokenManager = createTokenManager(tokenStorage, authRepository)
    val httpClient = createAuthenticatedHttpClient(tokenManager)
    val apiService: FeedbackApiService = KtorFeedbackApiService(httpClient, baseUrl)
    return FeedbackRepositoryImpl(apiService, networkConnectivity)
}