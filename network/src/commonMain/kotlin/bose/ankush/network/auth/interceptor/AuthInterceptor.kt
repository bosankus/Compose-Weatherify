package bose.ankush.network.auth.interceptor

import bose.ankush.network.auth.storage.TokenStorage
import bose.ankush.network.auth.token.TokenManager
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking

/**
 * Helper function to configure a HttpClient with authentication
 *
 * @param tokenManager The manager for JWT tokens
 * @return A configured HttpClient with authentication headers and token refresh
 */
fun HttpClientConfig<*>.configureAuth(tokenManager: TokenManager) {
    // Add authorization header to all requests
    defaultRequest {
        runBlocking {
            val token = tokenManager.getValidToken()
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }
    }

    // Handle 401 Unauthorized responses by refreshing the token
    HttpResponseValidator {
        handleResponseExceptionWithRequest { exception, request ->
            // Re-throw the exception to let the caller handle it
            throw exception
        }
    }

    // Add response observer to handle 401 responses
    install(ResponseObserver) {
        onResponse { response ->
            if (response.status == HttpStatusCode.Unauthorized) {
                // Log the 401 response
                println("[DEBUG_LOG] Received 401 Unauthorized response from ${response.request.url}")

                // Attempt to refresh the token
                runBlocking {
                    val refreshed = tokenManager.handleUnauthorized()
                    if (refreshed) {
                        println("[DEBUG_LOG] Token refreshed successfully after 401 response")
                    } else {
                        println("[DEBUG_LOG] Failed to refresh token after 401 response; forcing logout and notifying UI")
                        try {
                            // Clear token so that app considers user logged out
                            tokenManager.forceLogout()
                        } catch (_: Exception) {
                        }
                        // Emit a global unauthorized event for the UI to react (navigate to login + snackbar)
                        try {
                            bose.ankush.network.auth.events.AuthEventBus.emit(
                                bose.ankush.network.auth.events.AuthEvent.Unauthorized(
                                    message = "For security, please log in again to continue using the app."
                                )
                            )
                        } catch (e: Exception) {
                            println("[DEBUG_LOG] Failed to emit Unauthorized event: ${e.message}")
                        }
                    }
                }
            }
        }
    }
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