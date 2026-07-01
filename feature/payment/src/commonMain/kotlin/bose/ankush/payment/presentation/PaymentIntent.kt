package bose.ankush.payment.presentation

sealed interface PaymentIntent {
    data class StartPayment(
        val amountInPaisa: Long,
        val currency: String = "INR",
    ) : PaymentIntent

    data class VerifyPayment(
        val orderId: String,
        val paymentId: String,
        val signature: String,
    ) : PaymentIntent

    data class PaymentFailed(
        val message: String,
    ) : PaymentIntent
}
