package bose.ankush.payment.domain.repository

import bose.ankush.payment.domain.model.CreateOrderParams
import bose.ankush.payment.domain.model.Order
import bose.ankush.payment.domain.model.PaymentVerificationResult
import bose.ankush.payment.domain.model.VerifyPaymentParams

interface PaymentRepository {
    suspend fun createOrder(params: CreateOrderParams): Result<Order>

    suspend fun verifyPayment(params: VerifyPaymentParams): Result<PaymentVerificationResult>
}
