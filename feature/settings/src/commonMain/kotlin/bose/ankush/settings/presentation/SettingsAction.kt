package bose.ankush.settings.presentation

import bose.ankush.network.model.PricingTier
import bose.ankush.network.model.Service

internal sealed interface SettingsAction {
    data object OpenPremiumSheet : SettingsAction

    data object ClosePremiumSheet : SettingsAction

    data object OpenLogoutDialog : SettingsAction

    data object CloseLogoutDialog : SettingsAction

    data object ShowPremiumActivationToast : SettingsAction

    data object DismissPremiumActivationToast : SettingsAction

    data class OpenWebUrl(
        val url: String,
    ) : SettingsAction

    data object CloseWebView : SettingsAction

    data object ServicesLoading : SettingsAction

    data class ServicesLoaded(
        val services: List<Service>,
        val selectedService: Service?,
        val selectedTier: PricingTier?,
    ) : SettingsAction

    data class ServicesLoadFailed(
        val message: String,
    ) : SettingsAction

    data class ServiceSelected(
        val service: Service,
    ) : SettingsAction

    data class TierSelected(
        val tier: PricingTier,
    ) : SettingsAction

    data object ServiceSubscriptionReset : SettingsAction
}
