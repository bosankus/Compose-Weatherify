package bose.ankush.weatherify.domain.use_case.payment

import bose.ankush.network.model.VerifyPaymentRequest
import bose.ankush.network.model.VerifyPaymentResponse
import bose.ankush.network.repository.PaymentRepository
import javax.inject.Inject

class VerifyPayment @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(request: VerifyPaymentRequest): Result<VerifyPaymentResponse> =
        repository.verifyPayment(request)
}
