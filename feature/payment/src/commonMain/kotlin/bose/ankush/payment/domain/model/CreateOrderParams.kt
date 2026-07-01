package bose.ankush.payment.domain.model

data class CreateOrderParams(
    val amount: Long,
    val currency: String,
)
