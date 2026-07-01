package bose.ankush.payment.domain.model

data class VerifyPaymentParams(
    val orderId: String,
    val paymentId: String,
    val signature: String,
)
