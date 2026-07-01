package bose.ankush.payment.domain.usecase

import bose.ankush.payment.domain.model.PaymentVerificationResult
import bose.ankush.payment.domain.model.VerifyPaymentParams
import bose.ankush.payment.domain.repository.PaymentRepository

class VerifyPaymentUseCase(
    private val repository: PaymentRepository,
) {
    suspend operator fun invoke(params: VerifyPaymentParams): Result<PaymentVerificationResult> =
        repository.verifyPayment(params)
}
