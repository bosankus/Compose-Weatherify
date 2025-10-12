package bose.ankush.weatherify.domain.use_case.payment

import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.CreateOrderResponse
import bose.ankush.network.repository.PaymentRepository
import javax.inject.Inject

class CreateOrder @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(request: CreateOrderRequest): Result<CreateOrderResponse> =
        repository.createOrder(request)
}
