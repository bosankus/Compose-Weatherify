package bose.ankush.home.presentation.places

import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion

/** Saved places page of the home screen pager, plus the add-a-place search sheet. */
internal data class SavedPlacesState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val places: List<Location> = emptyList(),
    /** The pinned place Home is showing, or null while Home follows GPS. */
    val active: ActivePlace? = null,
    val notice: SavedPlacesNotice? = null,
    val search: SavedPlaceSearchState = SavedPlaceSearchState(),
)

internal data class ActivePlace(
    val lat: Double,
    val lon: Double,
    val name: String?,
)

internal data class SavedPlaceSearchState(
    val isOpen: Boolean = false,
    val query: String = "",
    val results: List<LocationSuggestion> = emptyList(),
    val isSearching: Boolean = false,
    val searchFailed: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
)

/** One-line feedback on the places page. */
internal enum class SavedPlacesNotice {
    DeleteFailed,
    SelectFailed,
}

internal sealed interface SavedPlacesIntent {
    data object Load : SavedPlacesIntent

    data class Select(
        val place: Location,
    ) : SavedPlacesIntent

    data object UseCurrentLocation : SavedPlacesIntent

    data class Delete(
        val id: String,
    ) : SavedPlacesIntent

    data object OpenSearch : SavedPlacesIntent

    data object CloseSearch : SavedPlacesIntent

    data class QueryChanged(
        val query: String,
    ) : SavedPlacesIntent

    data class SaveSuggestion(
        val suggestion: LocationSuggestion,
    ) : SavedPlacesIntent
}

internal sealed interface SavedPlacesEffect {
    /** The active location changed; the pager returns to the weather page. */
    data object ShowWeather : SavedPlacesEffect

    /** Another screen asked for saved places; the pager jumps to the places page. */
    data object ShowPlaces : SavedPlacesEffect
}
