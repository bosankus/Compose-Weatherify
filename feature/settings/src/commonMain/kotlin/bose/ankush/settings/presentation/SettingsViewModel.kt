package bose.ankush.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.network.repository.ServiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings' MVI ViewModel. Owns premium-sheet/logout-dialog/web-view UI state plus the inline
 * service subscription (plan picker) state, all reduced through [SettingsReducer].
 */
internal class SettingsViewModel(
    private val serviceRepository: ServiceRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.OpenPremiumSheet -> {
                analyticsTracker.track(AnalyticsEvent.UpgradePromptShown)
                dispatch(SettingsAction.OpenPremiumSheet)
            }

            SettingsIntent.ClosePremiumSheet -> {
                dispatch(SettingsAction.ClosePremiumSheet)
                dispatch(SettingsAction.ServiceSubscriptionReset)
            }

            SettingsIntent.OpenLogoutDialog -> dispatch(SettingsAction.OpenLogoutDialog)
            SettingsIntent.CloseLogoutDialog -> dispatch(SettingsAction.CloseLogoutDialog)
            SettingsIntent.PremiumActivated -> dispatch(SettingsAction.ShowPremiumActivationToast)
            SettingsIntent.DismissPremiumActivationToast -> dispatch(SettingsAction.DismissPremiumActivationToast)
            is SettingsIntent.OpenWebUrl -> dispatch(SettingsAction.OpenWebUrl(intent.url))
            SettingsIntent.CloseWebView -> dispatch(SettingsAction.CloseWebView)
            SettingsIntent.LoadServices -> loadServices()
            is SettingsIntent.SelectService -> {
                analyticsTracker.track(AnalyticsEvent.ServiceSelected(intent.service.id))
                dispatch(SettingsAction.ServiceSelected(intent.service))
            }
            is SettingsIntent.SelectTier -> {
                val serviceId = _state.value.serviceSubscription.selectedService?.id ?: ""
                analyticsTracker.track(AnalyticsEvent.TierSelected(serviceId, intent.tier.id))
                dispatch(SettingsAction.TierSelected(intent.tier))
            }
        }
    }

    private fun dispatch(action: SettingsAction) {
        _state.update { SettingsReducer.reduce(it, action) }
    }

    private fun loadServices() {
        viewModelScope.launch {
            dispatch(SettingsAction.ServicesLoading)

            serviceRepository.getServices().fold(
                onSuccess = { services ->
                    val available = services.filter { it.isAvailable }
                    val selectedService = available.firstOrNull()
                    dispatch(
                        SettingsAction.ServicesLoaded(
                            services = available,
                            selectedService = selectedService,
                            selectedTier = selectedService?.getRecommendedTier(),
                        ),
                    )
                },
                onFailure = { error ->
                    dispatch(SettingsAction.ServicesLoadFailed(userFriendlyErrorMessage(error)))
                },
            )
        }
    }

    private fun userFriendlyErrorMessage(error: Throwable): String =
        when {
            error.message?.contains("Illegal input", ignoreCase = true) == true ->
                "Unable to load subscription plans. Please try again."

            error.message?.contains("Network", ignoreCase = true) == true ->
                "Network connection issue. Please check your internet."

            error.message?.contains("404", ignoreCase = true) == true ->
                "Service not found. Please try again later."

            error.message?.contains("500", ignoreCase = true) == true ->
                "Server error. Please try again later."

            else -> "Unable to load subscription plans. Please try again."
        }
}
