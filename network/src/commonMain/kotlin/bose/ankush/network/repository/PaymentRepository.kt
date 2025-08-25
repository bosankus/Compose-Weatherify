package bose.ankush.network.repository

import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.CreateOrderResponse
import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.network.model.VerifyPaymentResponse

/**
 * Repository interface for payment operations
 */
interface PaymentRepository {
    /**
     * Create an order on backend which in turn calls Razorpay Orders API
     */
    suspend fun createOrder(request: CreateOrderRequest): CreateOrderResponse

    /**
     * Verify payment signature on backend
     */
    suspend fun verifyPayment(request: VerifyPaymentRequest): VerifyPaymentResponse
}