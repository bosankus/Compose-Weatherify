package bose.ankush.home.ios

import bose.ankush.home.presentation.places.SavedPlacesIntent
import bose.ankush.home.presentation.places.SavedPlacesViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Taps on the saved places page and the add-a-place sheet. Places and suggestions are named by
 * the keys in [PlaceUi] and [SuggestionUi], so Swift never holds the shared models themselves.
 */
class PlaceActions internal constructor(
    private val places: SavedPlacesViewModel,
    private val pickedSuggestion: MutableStateFlow<String?>,
    private val onPlacesPage: MutableStateFlow<Boolean>,
) {
    /** The pager settled on a page; the places page loads each time it is shown. */
    fun pageChanged(placesPage: Boolean) {
        onPlacesPage.value = placesPage
        if (placesPage) places.processIntent(SavedPlacesIntent.Load)
    }

    fun load() = places.processIntent(SavedPlacesIntent.Load)

    fun useCurrentLocation() = places.processIntent(SavedPlacesIntent.UseCurrentLocation)

    fun select(key: String) {
        places.state.value.places
            .findByKey(key)
            ?.let { places.processIntent(SavedPlacesIntent.Select(it)) }
    }

    fun delete(key: String) {
        places.state.value.places
            .findByKey(key)
            ?.let { places.processIntent(SavedPlacesIntent.Delete(it.id)) }
    }

    fun openSearch() {
        pickedSuggestion.value = null
        places.processIntent(SavedPlacesIntent.OpenSearch)
    }

    fun closeSearch() = places.processIntent(SavedPlacesIntent.CloseSearch)

    fun queryChanged(query: String) = places.processIntent(SavedPlacesIntent.QueryChanged(query))

    fun pick(key: String) {
        pickedSuggestion.value = key
    }

    /** Saves the picked place (or switches to it when it is already saved). */
    fun savePicked() {
        val key = pickedSuggestion.value ?: return
        places.state.value.search.results
            .firstOrNull { it.key() == key }
            ?.let { places.processIntent(SavedPlacesIntent.SaveSuggestion(it)) }
    }
}
