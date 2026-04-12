package bose.ankush.payment.domain.usecase

import bose.ankush.network.model.CreateOrderRequest
import bose.ankush.network.model.CreateOrderResponse
import bose.ankush.payment.domain.repository.PaymentRepository

class CreateOrderUseCase(private val repository: PaymentRepository) {
    suspend operator fun invoke(request: CreateOrderRequest): Result<CreateOrderResponse> =
        repository.createOrder(request)
}
