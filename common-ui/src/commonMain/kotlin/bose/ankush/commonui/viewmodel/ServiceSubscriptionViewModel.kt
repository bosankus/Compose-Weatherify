package bose.ankush.commonui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.network.model.PricingTier
import bose.ankush.network.model.Service
import bose.ankush.network.repository.ServiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServiceSubscriptionUiState(
    val isLoading: Boolean = true,
    val services: List<Service> = emptyList(),
    val selectedService: Service? = null,
    val selectedTier: PricingTier? = null,
    val error: String? = null,
    val message: String? = null,
)

class ServiceSubscriptionViewModel(
    private val repository: ServiceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServiceSubscriptionUiState())
    val uiState: StateFlow<ServiceSubscriptionUiState> = _uiState.asStateFlow()

    fun loadServices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getServices().fold(
                onSuccess = { services ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            services = services.filter { service -> service.isAvailable },
                            selectedService = services.firstOrNull { service -> service.isAvailable },
                            selectedTier =
                                services
                                    .firstOrNull { service -> service.isAvailable }
                                    ?.getRecommendedTier(),
                        )
                    }
                },
                onFailure = { error ->
                    val userMessage = getUserFriendlyErrorMessage(error)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = userMessage,
                        )
                    }
                },
            )
        }
    }

    private fun getUserFriendlyErrorMessage(error: Throwable): String =
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

    fun selectService(service: Service) {
        _uiState.update {
            it.copy(
                selectedService = service,
                selectedTier = service.getRecommendedTier(),
            )
        }
    }

    fun selectPricingTier(tier: PricingTier) {
        _uiState.update { it.copy(selectedTier = tier) }
    }

    fun resetState() {
        _uiState.update {
            ServiceSubscriptionUiState(isLoading = true)
        }
    }
}
