package bose.ankush.home.presentation.places

import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion

/** Saved places page of the Wander home pager, plus the add-a-place search sheet. */
internal data class WanderPlacesState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val places: List<Location> = emptyList(),
    /** The pinned place Home is showing, or null while Home follows GPS. */
    val active: ActivePlace? = null,
    val notice: WanderPlacesNotice? = null,
    val search: WanderPlaceSearchState = WanderPlaceSearchState(),
)

internal data class ActivePlace(
    val lat: Double,
    val lon: Double,
    val name: String?,
)

internal data class WanderPlaceSearchState(
    val isOpen: Boolean = false,
    val query: String = "",
    val results: List<LocationSuggestion> = emptyList(),
    val isSearching: Boolean = false,
    val searchFailed: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
)

/** One-line feedback on the places page. */
internal enum class WanderPlacesNotice {
    DeleteFailed,
    SelectFailed,
}

internal sealed interface WanderPlacesIntent {
    data object Load : WanderPlacesIntent

    data class Select(
        val place: Location,
    ) : WanderPlacesIntent

    data object UseCurrentLocation : WanderPlacesIntent

    data class Delete(
        val id: String,
    ) : WanderPlacesIntent

    data object OpenSearch : WanderPlacesIntent

    data object CloseSearch : WanderPlacesIntent

    data class QueryChanged(
        val query: String,
    ) : WanderPlacesIntent

    data class SaveSuggestion(
        val suggestion: LocationSuggestion,
    ) : WanderPlacesIntent
}

internal sealed interface WanderPlacesEffect {
    /** The active location changed; the pager returns to the weather page. */
    data object ShowWeather : WanderPlacesEffect

    /** Another screen asked for saved places; the pager jumps to the places page. */
    data object ShowPlaces : WanderPlacesEffect
}
