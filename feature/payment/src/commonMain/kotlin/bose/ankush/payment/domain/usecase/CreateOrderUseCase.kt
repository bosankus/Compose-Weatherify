package bose.ankush.payment.domain.usecase

import bose.ankush.payment.domain.model.CreateOrderParams
import bose.ankush.payment.domain.model.Order
import bose.ankush.payment.domain.repository.PaymentRepository

class CreateOrderUseCase(
    private val repository: PaymentRepository,
) {
    suspend operator fun invoke(params: CreateOrderParams): Result<Order> =
        repository.createOrder(params)
}
