package bose.ankush.settings.presentation

import bose.ankush.network.model.PricingTier
import bose.ankush.network.model.Service

internal sealed interface SettingsIntent {
    data object OpenPremiumSheet : SettingsIntent

    data object ClosePremiumSheet : SettingsIntent

    data object OpenLogoutDialog : SettingsIntent

    data object CloseLogoutDialog : SettingsIntent

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
