package bose.ankush.network.api

import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.Account
import bose.ankush.network.model.AccountPhoto
import bose.ankush.network.model.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class KtorAccountApiService(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager,
    private val baseUrl: String,
) : AccountApiService {
    override suspend fun getAccount(): ApiResponse<Account> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                get("$baseUrl/account") { authConfig() }
            }.body()

    override suspend fun getAccountPhoto(): ApiResponse<AccountPhoto> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                get("$baseUrl/account/photo") { authConfig() }
            }.body()
}
