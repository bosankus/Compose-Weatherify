package bose.ankush.payment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.payment.domain.config.PaymentConfig
import bose.ankush.payment.domain.model.CreateOrderParams
import bose.ankush.payment.domain.model.VerifyPaymentParams
import bose.ankush.payment.domain.store.PremiumStore
import bose.ankush.payment.domain.usecase.CreateOrderUseCase
import bose.ankush.payment.domain.usecase.VerifyPaymentUseCase
import bose.ankush.payment.generated.resources.Res
import bose.ankush.payment.generated.resources.payment_checkout_description
import bose.ankush.payment.generated.resources.payment_checkout_name
import bose.ankush.payment.generated.resources.payment_creating_order
import bose.ankush.payment.generated.resources.payment_error_cancelled
import bose.ankush.payment.generated.resources.payment_error_generic
import bose.ankush.payment.generated.resources.payment_error_network
import bose.ankush.payment.generated.resources.payment_error_timeout
import bose.ankush.payment.generated.resources.payment_order_created
import bose.ankush.payment.generated.resources.payment_unavailable
import bose.ankush.payment.generated.resources.payment_verified
import bose.ankush.payment.generated.resources.payment_verifying
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
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
                    message = getString(Res.string.payment_creating_order),
                    stage = PaymentStage.CreatingOrder,
                )
            }

            val key = paymentConfig.razorpayKey
            if (key.isBlank()) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        message = getString(Res.string.payment_unavailable),
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
                                    name = getString(Res.string.payment_checkout_name),
                                    description = getString(Res.string.payment_checkout_description),
                                ),
                            ),
                        )
                        _uiState.update {
                            it.copy(
                                loading = false,
                                message = getString(Res.string.payment_order_created),
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
                    message = getString(Res.string.payment_verifying),
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
                                message = getString(Res.string.payment_verified),
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
        viewModelScope.launch {
            val friendlyMessage = friendlyServerMessage(message)
            _uiState.update {
                it.copy(
                    loading = false,
                    message = friendlyMessage,
                    stage = PaymentStage.Failure,
                )
            }
        }
    }

    private suspend fun friendlyServerMessage(message: String?): String {
        if (message.isNullOrBlank()) return getString(Res.string.payment_error_generic)
        val lower = message.lowercase()
        return when {
            "timeout" in lower -> getString(Res.string.payment_error_timeout)
            "cancel" in lower -> getString(Res.string.payment_error_cancelled)
            "network" in lower || "unable to resolve host" in lower ->
                getString(Res.string.payment_error_network)
            else -> getString(Res.string.payment_error_generic)
        }
    }

    private suspend fun friendlyErrorMessage(t: Throwable?): String {
        if (t is CancellationException) throw t
        return friendlyServerMessage(t?.message)
    }
}
