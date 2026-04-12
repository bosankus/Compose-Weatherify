package bose.ankush.payment.domain.repository

import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.CreateOrderResponse
import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.network.model.VerifyPaymentResponse

interface PaymentRepository {
    suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse>
    suspend fun verifyPayment(request: VerifyPaymentRequest): Result<VerifyPaymentResponse>
}
