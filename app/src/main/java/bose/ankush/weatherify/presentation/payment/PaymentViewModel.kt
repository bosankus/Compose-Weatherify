package bose.ankush.weatherify.presentation.payment

/**
 * Payment UI models used across the app.
 * Note: This file intentionally contains no ViewModel to adhere to SRP.
 * - MainViewModel owns payment state and business logic.
 * - MainActivity owns platform-specific checkout invocation (Razorpay).
 * This separation avoids tight coupling and keeps UI models reusable.
 */

enum class PaymentStage { Idle, CreatingOrder, AwaitingPayment, Verifying, Success, Failure }

data class PaymentUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val stage: PaymentStage = PaymentStage.Idle,
    val isPremiumActivated: Boolean = false,
    val expiryMillis: Long? = null
)

sealed class PaymentEvent {
    data class LaunchCheckout(
        val keyId: String,
        val orderId: String,
        val amount: Long,
        val currency: String,
        val name: String,
        val description: String,
        val email: String? = null,
        val contact: String? = null
    ) : PaymentEvent()
}
