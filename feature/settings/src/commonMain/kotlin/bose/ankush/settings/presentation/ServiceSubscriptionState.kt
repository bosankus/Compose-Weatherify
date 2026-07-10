package bose.ankush.settings.presentation

import bose.ankush.network.model.PricingTier
import bose.ankush.network.model.Service

internal data class ServiceSubscriptionState(
    val isLoading: Boolean = true,
    val services: List<Service> = emptyList(),
    val selectedService: Service? = null,
    val selectedTier: PricingTier? = null,
    val error: String? = null,
)
