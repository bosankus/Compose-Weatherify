package bose.ankush.weatherify.presentation

import androidx.lifecycle.ViewModel
import bose.ankush.commonui.settings.SettingsScreenState
import bose.ankush.commonui.viewmodel.ServiceSubscriptionViewModel
import bose.ankush.network.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed class SettingsEvent {
    data object OpenPremiumSheet : SettingsEvent()

    data object ClosePremiumSheet : SettingsEvent()

    data object OpenLogoutDialog : SettingsEvent()

    data object CloseLogoutDialog : SettingsEvent()

    data object DismissPremiumToast : SettingsEvent()

    data class OpenWebUrl(
        val url: String,
    ) : SettingsEvent()

    data object CloseWebView : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val serviceRepository: ServiceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsScreenState())
    val uiState: StateFlow<SettingsScreenState> = _uiState

    val serviceSubscriptionViewModel by lazy {
        ServiceSubscriptionViewModel(repository = serviceRepository)
    }

    fun handleEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.OpenPremiumSheet -> {
                _uiState.value = _uiState.value.copy(showPremiumBottomSheet = true)
            }

            SettingsEvent.ClosePremiumSheet -> {
                _uiState.value = _uiState.value.copy(showPremiumBottomSheet = false)
            }

            SettingsEvent.OpenLogoutDialog -> {
                _uiState.value = _uiState.value.copy(showLogoutDialog = true)
            }

            SettingsEvent.CloseLogoutDialog -> {
                _uiState.value = _uiState.value.copy(showLogoutDialog = false)
            }

            SettingsEvent.DismissPremiumToast -> {
                _uiState.value = _uiState.value.copy(showPremiumActivationToast = false)
            }

            is SettingsEvent.OpenWebUrl -> {
                _uiState.value = _uiState.value.copy(currentWebUrl = event.url)
            }

            SettingsEvent.CloseWebView -> {
                _uiState.value = _uiState.value.copy(currentWebUrl = null)
            }
        }
    }

    fun showPremiumActivationToast() {
        _uiState.value = _uiState.value.copy(showPremiumActivationToast = true)
    }
}
