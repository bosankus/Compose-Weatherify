package bose.ankush.payment.presentation

enum class PaymentStage { Idle, CreatingOrder, AwaitingPayment, Verifying, Success, Failure }

data class PaymentUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val stage: PaymentStage = PaymentStage.Idle,
    val isPremiumActivated: Boolean = false,
    val expiryMillis: Long? = null,
)

/**
 * Parameters for launching the platform-specific payment checkout (e.g. Razorpay on Android).
 * Emitted by [PaymentViewModel] after a successful order creation; consumed by the UI layer.
 */
data class CheckoutParams(
    val keyId: String,
    val orderId: String,
    val amount: Long,
    val currency: String,
    val name: String,
    val description: String,
    val email: String? = null,
    val contact: String? = null,
)
