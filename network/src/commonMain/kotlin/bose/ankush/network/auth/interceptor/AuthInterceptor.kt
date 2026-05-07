@file:Suppress("ktlint:standard:max-line-length")

package bose.ankush.network.auth.interceptor

import bose.ankush.network.auth.events.AuthEvent
import bose.ankush.network.auth.events.AuthEventBus
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.auth.token.TokenResult
import bose.ankush.storage.api.TokenStorage
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking

/**
 * Helper function to configure a HttpClient with authentication
 *
 * @param tokenManager The manager for JWT tokens
 * @return A configured HttpClient with authentication headers and token refresh
 */
fun HttpClientConfig<*>.configureAuth(tokenManager: TokenManager) {
    install(
        createClientPlugin("AuthTokenPlugin") {
            on(Send) { request ->
                // Attach stored token before sending — no proactive refresh here
                tokenManager.getStoredToken()?.takeIf { it.isNotBlank() }?.let { token ->
                    request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                }

                val originalCall = proceed(request)

                // On 401, refresh token and retry once
                if (originalCall.response.status == HttpStatusCode.Unauthorized) {
                    when (val refreshResult = tokenManager.handleUnauthorized()) {
                        is TokenResult.Valid -> {
                            request.headers.remove(HttpHeaders.Authorization)
                            request.headers.append(
                                HttpHeaders.Authorization,
                                "Bearer ${refreshResult.token}",
                            )
                            proceed(request)
                        }

                        is TokenResult.Error -> {
                            val event =
                                AuthEvent.Unauthorized(
                                    message = "Network error during re-authentication: ${refreshResult.exception.message}",
                                )
                            AuthEventBus.tryEmit(event)
                            originalCall
                        }

                        is TokenResult.InvalidToken, is TokenResult.NoToken -> {
                            tokenManager.forceLogout()
                            val event =
                                AuthEvent.Unauthorized(
                                    message = "For security, please log in again to continue using the app.",
                                )
                            AuthEventBus.tryEmit(event)
                            originalCall
                        }
                    }
                } else {
                    originalCall
                }
            }
        },
    )
}

/**
 * Legacy helper function to maintain backward compatibility
 * @param tokenStorage The storage for authentication tokens
 */
fun HttpClientConfig<*>.configureAuth(tokenStorage: TokenStorage) {
    defaultRequest {
        runBlocking {
            val token = tokenStorage.getToken()
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }
    }
}
