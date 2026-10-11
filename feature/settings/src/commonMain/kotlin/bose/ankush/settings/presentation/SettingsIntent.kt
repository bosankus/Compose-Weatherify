package bose.ankush.settings.presentation

import bose.ankush.network.model.PricingTier
import bose.ankush.network.model.Service

internal sealed interface SettingsIntent {
    data object OpenPremiumSheet : SettingsIntent

    data object ClosePremiumSheet : SettingsIntent

    data object OpenLogoutDialog : SettingsIntent

    data object CloseLogoutDialog : SettingsIntent

    data object OpenDeleteAccountDialog : SettingsIntent

    /** Ignored while the request is in flight. */
    data object CloseDeleteAccountDialog : SettingsIntent

    /** Clears the last error, so it doesn't sit under a password the user is retyping. */
    data object DeleteAccountPasswordEdited : SettingsIntent

    data class ConfirmDeleteAccount(
        val password: String,
    ) : SettingsIntent

    data object PremiumActivated : SettingsIntent

    data object DismissPremiumActivationToast : SettingsIntent

    data class OpenWebUrl(
        val url: String,
    ) : SettingsIntent

    data object CloseWebView : SettingsIntent

    data object LoadServices : SettingsIntent

    data class SelectService(
        val service: Service,
    ) : SettingsIntent

    data class SelectTier(
        val tier: PricingTier,
    ) : SettingsIntent
}
