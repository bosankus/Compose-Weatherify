package bose.ankush.network.auth.interceptor

import bose.ankush.network.auth.events.AuthEvent
import bose.ankush.network.auth.events.AuthEventBus
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.auth.token.TokenResult
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

suspend fun HttpClient.authorizedRequest(
    tokenManager: TokenManager,
    block: suspend HttpClient.(HttpRequestBuilder.() -> Unit) -> HttpResponse,
): HttpResponse {
    fun authHeader(token: String?): HttpRequestBuilder.() -> Unit =
        {
            token?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }

    val firstToken = tokenManager.getStoredToken()
    val response = block(authHeader(firstToken))

    if (response.status != HttpStatusCode.Unauthorized) {
        return response
    }

    // Reacts to 401 only: a 403 (e.g. a wrong password on DELETE /account) never refreshes or
    // signs out. At most one refresh and one replay per call, so a 401 can't loop.
    return when (val result = tokenManager.handleUnauthorized()) {
        is TokenResult.Valid -> {
            val retried = block(authHeader(result.token))
            // A fresh token that is still refused means the account itself is gone or
            // deactivated; refreshing again would only repeat this.
            if (retried.status == HttpStatusCode.Unauthorized) endSession(tokenManager)
            retried
        }

        is TokenResult.Error -> {
            AuthEventBus.tryEmit(
                AuthEvent.Unauthorized(
                    "Network error during re-authentication: ${result.exception.message}",
                ),
            )
            response
        }

        is TokenResult.InvalidToken, is TokenResult.NoToken -> {
            endSession(tokenManager)
            response
        }
    }
}

private suspend fun endSession(tokenManager: TokenManager) {
    tokenManager.forceLogout()
    AuthEventBus.tryEmit(
        AuthEvent.Unauthorized(
            "For security, please log in again to continue using the app.",
        ),
    )
}
