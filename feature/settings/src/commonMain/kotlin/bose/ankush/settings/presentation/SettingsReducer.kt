package bose.ankush.settings.presentation

internal object SettingsReducer {
    fun reduce(
        state: SettingsState,
        action: SettingsAction,
    ): SettingsState =
        when (action) {
            SettingsAction.OpenPremiumSheet -> state.copy(showPremiumBottomSheet = true)

            SettingsAction.ClosePremiumSheet -> state.copy(showPremiumBottomSheet = false)

            SettingsAction.OpenLogoutDialog -> state.copy(showLogoutDialog = true)

            SettingsAction.CloseLogoutDialog -> state.copy(showLogoutDialog = false)

            SettingsAction.ShowPremiumActivationToast -> state.copy(showPremiumActivationToast = true)

            SettingsAction.DismissPremiumActivationToast -> state.copy(showPremiumActivationToast = false)

            is SettingsAction.OpenWebUrl -> state.copy(currentWebUrl = action.url)

            SettingsAction.CloseWebView -> state.copy(currentWebUrl = null)

            SettingsAction.ServicesLoading ->
                state.copy(
                    serviceSubscription = state.serviceSubscription.copy(isLoading = true, error = null),
                )

            is SettingsAction.ServicesLoaded ->
                state.copy(
                    serviceSubscription =
                        state.serviceSubscription.copy(
                            isLoading = false,
                            services = action.services,
                            selectedService = action.selectedService,
                            selectedTier = action.selectedTier,
                        ),
                )

            is SettingsAction.ServicesLoadFailed ->
                state.copy(
                    serviceSubscription =
                        state.serviceSubscription.copy(isLoading = false, error = action.message),
                )

            is SettingsAction.ServiceSelected ->
                state.copy(
                    serviceSubscription =
                        state.serviceSubscription.copy(
                            selectedService = action.service,
                            selectedTier = action.service.getRecommendedTier(),
                        ),
                )

            is SettingsAction.TierSelected ->
                state.copy(serviceSubscription = state.serviceSubscription.copy(selectedTier = action.tier))

            SettingsAction.ServiceSubscriptionReset ->
                state.copy(serviceSubscription = ServiceSubscriptionState())
        }
}
