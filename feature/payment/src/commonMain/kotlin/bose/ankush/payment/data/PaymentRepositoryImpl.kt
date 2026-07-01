package bose.ankush.payment.data

import bose.ankush.network.api.PaymentApiService
import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.network.util.NetworkConnectivity
import bose.ankush.payment.domain.model.CreateOrderParams
import bose.ankush.payment.domain.model.Order
import bose.ankush.payment.domain.model.PaymentVerificationResult
import bose.ankush.payment.domain.model.VerifyPaymentParams
import bose.ankush.payment.domain.repository.PaymentRepository
import kotlin.time.Clock

internal class PaymentRepositoryImpl(
    private val apiService: PaymentApiService,
    private val networkConnectivity: NetworkConnectivity,
) : PaymentRepository {
    override suspend fun createOrder(params: CreateOrderParams): Result<Order> {
        if (!networkConnectivity.isNetworkAvailable()) {
            return Result.failure(IllegalStateException("No internet connection"))
        }
        return runCatching {
            val request =
                CreateOrderRequest(
                    amount = params.amount,
                    currency = params.currency,
                    receipt = "receipt_${Clock.System.now().toEpochMilliseconds()}",
                    partialPayment = true,
                    firstPaymentMinAmount = 500L,
                )
            val response = apiService.createOrder(request)
            response.toOrder()
                ?: error("Order data missing or invalid in server response")
        }
    }

    override suspend fun verifyPayment(params: VerifyPaymentParams): Result<PaymentVerificationResult> {
        if (!networkConnectivity.isNetworkAvailable()) {
            return Result.failure(IllegalStateException("No internet connection"))
        }
        return runCatching {
            val request =
                VerifyPaymentRequest(
                    razorpayOrderId = params.orderId,
                    razorpayPaymentId = params.paymentId,
                    razorpaySignature = params.signature,
                )
            val response = apiService.verifyPayment(request)
            PaymentVerificationResult(
                success = response.success,
                message = response.message,
            )
        }
    }
}
