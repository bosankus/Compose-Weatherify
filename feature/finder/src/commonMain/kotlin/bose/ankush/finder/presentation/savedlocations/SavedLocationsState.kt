package bose.ankush.finder.presentation.savedlocations

import bose.ankush.finder.domain.model.Location

internal data class SavedLocationsState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    val locations: List<Location> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
)
