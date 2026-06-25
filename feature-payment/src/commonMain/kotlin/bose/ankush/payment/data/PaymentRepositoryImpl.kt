package bose.ankush.payment.data

import bose.ankush.network.api.PaymentApiService
import bose.ankush.network.util.NetworkConnectivity
import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.CreateOrderResponse
import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.network.model.VerifyPaymentResponse
import bose.ankush.payment.domain.repository.PaymentRepository

internal class PaymentRepositoryImpl(
    private val apiService: PaymentApiService,
    private val networkConnectivity: NetworkConnectivity,
) : PaymentRepository {
    override suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse> {
        if (!networkConnectivity.isNetworkAvailable()) {
            return Result.failure(IllegalStateException("No internet connection"))
        }
        return runCatching { apiService.createOrder(request) }
    }

    override suspend fun verifyPayment(request: VerifyPaymentRequest): Result<VerifyPaymentResponse> {
        if (!networkConnectivity.isNetworkAvailable()) {
            return Result.failure(IllegalStateException("No internet connection"))
        }
        return runCatching { apiService.verifyPayment(request) }
    }
}
