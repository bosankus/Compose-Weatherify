package bose.ankush.settings.presentation

import bose.ankush.network.model.PricingTier
import bose.ankush.network.model.Service

internal data class SettingsState(
    val showPremiumBottomSheet: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val showPremiumActivationToast: Boolean = false,
    val currentWebUrl: String? = null,
    val serviceSubscription: ServiceSubscriptionState = ServiceSubscriptionState(),
)
