package bose.ankush.payment.presentation

sealed interface PaymentEffect {
    data class LaunchCheckout(
        val params: CheckoutParams,
    ) : PaymentEffect
}
