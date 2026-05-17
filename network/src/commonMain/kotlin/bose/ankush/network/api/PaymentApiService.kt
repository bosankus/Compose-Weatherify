package bose.ankush.network.api

import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.CreateOrderResponse
import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.network.model.VerifyPaymentResponse

interface PaymentApiService {
    suspend fun createOrder(request: CreateOrderRequest): CreateOrderResponse

    suspend fun verifyPayment(request: VerifyPaymentRequest): VerifyPaymentResponse
}
