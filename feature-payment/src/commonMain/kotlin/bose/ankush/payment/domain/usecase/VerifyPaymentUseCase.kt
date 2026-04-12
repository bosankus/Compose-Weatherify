package bose.ankush.payment.domain.usecase

import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.network.model.VerifyPaymentResponse
import bose.ankush.payment.domain.repository.PaymentRepository

class VerifyPaymentUseCase(private val repository: PaymentRepository) {
    suspend operator fun invoke(request: VerifyPaymentRequest): Result<VerifyPaymentResponse> =
        repository.verifyPayment(request)
}
