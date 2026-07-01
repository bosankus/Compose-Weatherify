package bose.ankush.payment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.payment.domain.config.PaymentConfig
import bose.ankush.payment.domain.model.CreateOrderParams
import bose.ankush.payment.domain.model.VerifyPaymentParams
import bose.ankush.payment.domain.store.PremiumStore
import bose.ankush.payment.domain.usecase.CreateOrderUseCase
import bose.ankush.payment.domain.usecase.VerifyPaymentUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class PaymentViewModel(
    private val createOrderUseCase: CreateOrderUseCase,
    private val verifyPaymentUseCase: VerifyPaymentUseCase,
    private val premiumStore: PremiumStore,
    private val paymentConfig: PaymentConfig,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _effect = Channel<PaymentEffect>(Channel.BUFFERED)
    val effect: Flow<PaymentEffect> = _effect.receiveAsFlow()

    private val _checkoutParams = Channel<CheckoutParams>(Channel.BUFFERED)
    val checkoutParams: Flow<CheckoutParams> = _checkoutParams.receiveAsFlow()

    init {
        observePremiumStatus()
    }

    fun processIntent(intent: PaymentIntent) {
        when (intent) {
            is PaymentIntent.StartPayment ->
                handleStartPayment(
                    intent.amountInPaisa,
                    intent.currency,
                )
            is PaymentIntent.VerifyPayment ->
                handleVerifyPayment(
                    intent.orderId,
                    intent.paymentId,
                    intent.signature,
                )
            is PaymentIntent.PaymentFailed -> handlePaymentFailed(intent.message)
        }
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            premiumStore.observePremiumStatus().collect { status ->
                val now = Clock.System.now().toEpochMilliseconds()
                val isActive = status.expiryMillis != null && status.expiryMillis > now
                _uiState.update {
                    it.copy(
                        isPremiumActivated = isActive,
                        expiryMillis = status.expiryMillis,
                        stage = if (isActive) PaymentStage.Success else it.stage,
                    )
                }
            }
        }
    }

    private fun handleStartPayment(
        amountPaise: Long = 10_000L,
        currency: String = "INR",
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    message = "Creating order...",
                    stage = PaymentStage.CreatingOrder,
                )
            }

            val key = paymentConfig.razorpayKey
            if (key.isBlank()) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        message = "Payment is temporarily unavailable. Please try again later.",
                        stage = PaymentStage.Failure,
                    )
                }
                return@launch
            }

            createOrderUseCase(CreateOrderParams(amount = amountPaise, currency = currency))
                .fold(
                    onSuccess = { order ->
                        _effect.trySend(
                            PaymentEffect.LaunchCheckout(
                                CheckoutParams(
                                    keyId = key,
                                    orderId = order.orderId,
                                    amount = order.amount,
                                    currency = order.currency,
                                    name = "Weatherify Subscription",
                                    description = "Premium Plan",
                                ),
                            ),
                        )
                        _uiState.update {
                            it.copy(
                                loading = false,
                                message = "Order created",
                                stage = PaymentStage.AwaitingPayment,
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                loading = false,
                                message = friendlyErrorMessage(e),
                                stage = PaymentStage.Failure,
                            )
                        }
                    },
                )
        }
    }

    private fun handleVerifyPayment(
        orderId: String,
        paymentId: String,
        signature: String,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    message = "Verifying payment...",
                    stage = PaymentStage.Verifying,
                )
            }

            verifyPaymentUseCase(VerifyPaymentParams(orderId, paymentId, signature))
                .fold(
                    onSuccess = { result ->
                        if (!result.success) {
                            _uiState.update {
                                it.copy(
                                    loading = false,
                                    message = friendlyServerMessage(result.message),
                                    stage = PaymentStage.Failure,
                                )
                            }
                            return@fold
                        }
                        val expiryMillis =
                            Clock.System
                                .now()
                                .plus(30.days)
                                .toEpochMilliseconds()
                        premiumStore.savePremiumStatus(isPremium = true, expiryMillis = expiryMillis)
                        // _uiState auto-updates via observePremiumStatus() collecting the new value
                        _uiState.update {
                            it.copy(
                                loading = false,
                                message = "Payment verified",
                                stage = PaymentStage.Success,
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                loading = false,
                                message = friendlyErrorMessage(e),
                                stage = PaymentStage.Failure,
                            )
                        }
                    },
                )
        }
    }

    private fun handlePaymentFailed(message: String) {
        _uiState.update {
            it.copy(
                loading = false,
                message = friendlyServerMessage(message),
                stage = PaymentStage.Failure,
            )
        }
    }

    private fun friendlyServerMessage(message: String?): String {
        if (message.isNullOrBlank()) return "Something went wrong. Please try again."
        val lower = message.lowercase()
        return when {
            "timeout" in lower -> "The server took too long to respond. Please try again."
            "cancel" in lower -> "Payment was cancelled."
            "network" in lower || "unable to resolve host" in lower ->
                "Please check your internet connection and try again."
            else -> "Something went wrong. Please try again."
        }
    }

    private fun friendlyErrorMessage(t: Throwable?): String {
        if (t is CancellationException) throw t
        return friendlyServerMessage(t?.message)
    }
}
