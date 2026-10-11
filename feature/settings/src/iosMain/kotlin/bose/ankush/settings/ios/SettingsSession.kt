package bose.ankush.settings.ios

import bose.ankush.payment.presentation.PaymentUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * What Settings needs from the app's sign-in and payment state. The iOS app's session implements
 * it, so this module never depends on the app or on the auth feature.
 */
interface SettingsSession {
    val payment: StateFlow<PaymentUiState>
    val isLoggingOut: StateFlow<Boolean>

    fun logout()

    /** The session refresh is the only call that returns premium status and expiry. */
    fun refreshPremium()

    fun startPayment(amountPaise: Long)
}
