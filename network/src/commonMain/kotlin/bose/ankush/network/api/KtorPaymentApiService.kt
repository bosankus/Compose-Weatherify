package bose.ankush.network.api

import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.CreateOrderResponse
import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.network.model.VerifyPaymentResponse
import bose.ankush.network.utils.NetworkUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorPaymentApiService(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager,
    private val baseUrl: String,
) : PaymentApiService {
    override suspend fun createOrder(request: CreateOrderRequest): CreateOrderResponse =
        NetworkUtils.retryWithExponentialBackoff {
            httpClient
                .authorizedRequest(tokenManager) { authConfig ->
                    post("$baseUrl/create-order") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                        authConfig()
                    }
                }.body()
        }

    override suspend fun verifyPayment(request: VerifyPaymentRequest): VerifyPaymentResponse =
        NetworkUtils.retryWithExponentialBackoff {
            httpClient
                .authorizedRequest(tokenManager) { authConfig ->
                    post("$baseUrl/store-payment") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                        authConfig()
                    }
                }.body()
        }
}
