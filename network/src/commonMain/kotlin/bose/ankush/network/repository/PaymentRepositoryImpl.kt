package bose.ankush.network.repository

import bose.ankush.network.api.PaymentApiService
import bose.ankush.network.common.NetworkConnectivity
import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.CreateOrderResponse
import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.network.model.VerifyPaymentResponse

/**
 * Implementation of PaymentRepository
 */
class PaymentRepositoryImpl(
    private val apiService: PaymentApiService,
    private val networkConnectivity: NetworkConnectivity
) : PaymentRepository {

    override suspend fun createOrder(request: CreateOrderRequest): CreateOrderResponse {
        if (!networkConnectivity.isNetworkAvailable()) {
            throw IllegalStateException("No internet connection")
        }
        return apiService.createOrder(request)
    }

    override suspend fun verifyPayment(request: VerifyPaymentRequest): VerifyPaymentResponse {
        if (!networkConnectivity.isNetworkAvailable()) {
            throw IllegalStateException("No internet connection")
        }
        return apiService.verifyPayment(request)
    }
}