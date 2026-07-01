package bose.ankush.payment.domain.model

data class Order(
    val orderId: String,
    val amount: Long,
    val currency: String,
)
