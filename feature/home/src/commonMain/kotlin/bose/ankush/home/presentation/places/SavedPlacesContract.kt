package bose.ankush.home.presentation.places

import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.home.domain.model.ForecastPlace

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

/** How far Home has got with the forecast, so the selected card can show it is still loading. */
internal data class HomeProgress(
    /** The place Home's forecast is for; null until one is known. */
    val place: ForecastPlace? = null,
    val isLoading: Boolean = false,
    /** The latest attempt failed (offline, or no location). */
    val failed: Boolean = false,
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
    /** Fetch the list again: pull to refresh, retry. */
    data object Load : SavedPlacesIntent

    /**
     * The places page came into view. Fetches only while nothing has loaded yet (or the last
     * try failed): saves and deletes keep the list current, so paging past it costs no call.
     */
    data object PageShown : SavedPlacesIntent

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
    /** Another screen asked for saved places; the pager jumps to the places page. */
    data object ShowPlaces : SavedPlacesEffect
}
