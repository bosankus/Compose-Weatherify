package bose.ankush.finder.presentation.placesearch

import bose.ankush.finder.domain.model.LocationSuggestion

internal data class PlaceSearchState(
    val searchQuery: String = "",
    val results: List<LocationSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
