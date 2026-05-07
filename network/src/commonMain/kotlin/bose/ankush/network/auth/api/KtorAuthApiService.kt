package bose.ankush.network.auth.api

import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.model.LoginRequest
import bose.ankush.network.auth.model.LogoutResponse
import bose.ankush.network.auth.model.RefreshTokenRequest
import bose.ankush.network.auth.model.RegisterRequest
import bose.ankush.network.utils.NetworkUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Ktor implementation of AuthApiService
 */
class KtorAuthApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : AuthApiService {
    override suspend fun login(request: LoginRequest): AuthResponse =
        NetworkUtils.retryWithExponentialBackoff {
            httpClient
                .post("$baseUrl/login") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }

    override suspend fun register(request: RegisterRequest): AuthResponse =
        NetworkUtils.retryWithExponentialBackoff {
            httpClient
                .post("$baseUrl/register") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }

    override suspend fun refreshToken(request: RefreshTokenRequest): AuthResponse =
        NetworkUtils.retryWithExponentialBackoff {
            httpClient
                .post("$baseUrl/refresh-token") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }

    override suspend fun logout(): LogoutResponse =
        NetworkUtils.retryWithExponentialBackoff {
            httpClient.post("$baseUrl/logout").body()
        }
}
