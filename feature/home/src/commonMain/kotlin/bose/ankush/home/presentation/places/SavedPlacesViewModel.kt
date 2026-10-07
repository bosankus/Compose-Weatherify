package bose.ankush.home.presentation.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion
import bose.ankush.finder.domain.usecase.DeleteLocationUseCase
import bose.ankush.finder.domain.usecase.GetSavedLocationsUseCase
import bose.ankush.finder.domain.usecase.SaveLocationParams
import bose.ankush.finder.domain.usecase.SaveLocationUseCase
import bose.ankush.finder.domain.usecase.SearchPlacesUseCase
import bose.ankush.home.data.HomeSavedPlacesEntryImpl
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.api.PremiumStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Saved places page and add-a-place sheet on the home screen pager.
 *
 * Saved places come from the same finder use cases the old saved locations screen used.
 * Making a place active writes the location override to [LocationPreferencesStorage];
 * [bose.ankush.home.presentation.HomeViewModel] observes that override and refetches
 * the forecast for the new coordinates, so nothing here talks to the weather layer.
 * Requests from the app's saved locations tab ([HomeSavedPlacesEntryImpl]) open this page.
 */
@OptIn(FlowPreview::class)
internal class SavedPlacesViewModel(
    private val getSavedLocations: GetSavedLocationsUseCase,
    private val saveLocation: SaveLocationUseCase,
    private val deleteLocation: DeleteLocationUseCase,
    private val searchPlaces: SearchPlacesUseCase,
    private val premiumStorage: PremiumStorage,
    private val locationPreferences: LocationPreferencesStorage,
    private val analyticsTracker: AnalyticsTracker,
    private val savedPlacesEntry: HomeSavedPlacesEntryImpl,
) : ViewModel() {
    private val _state = MutableStateFlow(SavedPlacesState())
    val state: StateFlow<SavedPlacesState> = _state.asStateFlow()

    private val _effect = Channel<SavedPlacesEffect>(Channel.BUFFERED)
    val effect: Flow<SavedPlacesEffect> = _effect.receiveAsFlow()

    private val queries = MutableStateFlow("")
    private var loadJob: Job? = null
    private var hasTrackedSearchStart = false

    init {
        savedPlacesEntry.markAvailable()
        viewModelScope.launch {
            savedPlacesEntry.pending.filter { it }.collect {
                savedPlacesEntry.consume()
                _effect.trySend(SavedPlacesEffect.ShowPlaces)
                load()
            }
        }
        viewModelScope.launch {
            premiumStorage.observePremiumStatus().collect { status ->
                val becamePremium = status.isPremium && !_state.value.isPremium
                _state.update { it.copy(isPremium = status.isPremium) }
                if (becamePremium) load()
            }
        }
        viewModelScope.launch {
            locationPreferences
                .getLocationPreferencesFlow()
                .map(SavedPlacesLogic::activePlace)
                .distinctUntilChanged()
                .collect { active -> _state.update { it.copy(active = active) } }
        }
        viewModelScope.launch {
            queries
                .debounce(SEARCH_DEBOUNCE)
                .collectLatest { query -> search(query.trim()) }
        }
    }

    fun processIntent(intent: SavedPlacesIntent) {
        if (_state.value.notice != null) _state.update { it.copy(notice = null) }
        when (intent) {
            SavedPlacesIntent.Load -> load()
            is SavedPlacesIntent.Select -> select(intent.place)
            SavedPlacesIntent.UseCurrentLocation -> useCurrentLocation()
            is SavedPlacesIntent.Delete -> delete(intent.id)
            SavedPlacesIntent.OpenSearch -> resetSearch(open = true)
            SavedPlacesIntent.CloseSearch -> resetSearch(open = false)
            is SavedPlacesIntent.QueryChanged -> onQueryChanged(intent.query)
            is SavedPlacesIntent.SaveSuggestion -> save(intent.suggestion)
        }
    }

    private fun load() {
        if (!_state.value.isPremium) return
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, loadFailed = false) }
                getSavedLocations().fold(
                    onSuccess = { places ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                places = SavedPlacesLogic.distinctPlaces(places)
                            )
                        }
                    },
                    onFailure = { e ->
                        if (e is CancellationException) throw e
                        _state.update { it.copy(isLoading = false, loadFailed = true) }
                    },
                )
            }
    }

    private fun select(place: Location) {
        if (SavedPlacesLogic.isActive(place, _state.value.active)) {
            _effect.trySend(SavedPlacesEffect.ShowWeather)
            return
        }
        viewModelScope.launch {
            if (activate(place.lat, place.lon, place.name)) {
                analyticsTracker.track(AnalyticsEvent.LocationSelected(SELECT_SOURCE))
                _effect.trySend(SavedPlacesEffect.ShowWeather)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun useCurrentLocation() {
        if (_state.value.active == null) {
            _effect.trySend(SavedPlacesEffect.ShowWeather)
            return
        }
        viewModelScope.launch {
            try {
                locationPreferences.clearLocationOverride()
                _effect.trySend(SavedPlacesEffect.ShowWeather)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(notice = SavedPlacesNotice.SelectFailed) }
            }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            deleteLocation(id).fold(
                onSuccess = { _state.update { state -> state.copy(places = state.places.filterNot { it.id == id }) } },
                onFailure = { e ->
                    if (e is CancellationException) throw e
                    _state.update { it.copy(notice = SavedPlacesNotice.DeleteFailed) }
                },
            )
        }
    }

    private fun resetSearch(open: Boolean) {
        hasTrackedSearchStart = false
        _state.update { it.copy(search = SavedPlaceSearchState(isOpen = open)) }
        queries.value = ""
    }

    private fun onQueryChanged(query: String) {
        val searchable = SavedPlacesLogic.isSearchable(query)
        _state.update { state ->
            state.copy(
                search =
                    state.search.copy(
                        query = query,
                        isSearching = searchable,
                        searchFailed = false,
                        saveFailed = false,
                        results = if (searchable) state.search.results else emptyList(),
                    ),
            )
        }
        if (searchable && !hasTrackedSearchStart) {
            hasTrackedSearchStart = true
            analyticsTracker.track(AnalyticsEvent.LocationSearchStarted)
        }
        queries.value = query
    }

    private suspend fun search(query: String) {
        if (!SavedPlacesLogic.isSearchable(query)) {
            updateSearch { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        updateSearch { it.copy(isSearching = true, searchFailed = false) }
        searchPlaces(query).fold(
            onSuccess = { results ->
                analyticsTracker.track(AnalyticsEvent.Search(query))
                updateSearch { it.copy(results = results, isSearching = false) }
            },
            onFailure = { e ->
                if (e is CancellationException) throw e
                updateSearch { it.copy(isSearching = false, searchFailed = true) }
            },
        )
    }

    /**
     * Saves the place on the account, makes it the active location, closes the sheet, and
     * sends the pager back to the weather page. A place that is already saved is only
     * made active, so tapping it twice never stores a duplicate.
     */
    private fun save(suggestion: LocationSuggestion) {
        val current = _state.value
        if (current.search.isSaving) return
        val existing = SavedPlacesLogic.findSaved(suggestion, current.places)
        val place =
            existing ?: Location(
                id = "",
                name = SavedPlacesLogic.savedName(suggestion),
                lat = suggestion.latitude,
                lon = suggestion.longitude,
            )
        viewModelScope.launch {
            if (existing == null) {
                updateSearch { it.copy(isSaving = true, saveFailed = false) }
                val saved = saveLocation(SaveLocationParams(place.name, place.lat, place.lon))
                saved.exceptionOrNull()?.let { e ->
                    if (e is CancellationException) throw e
                    updateSearch { it.copy(isSaving = false, saveFailed = true) }
                    return@launch
                }
                analyticsTracker.track(AnalyticsEvent.LocationSaved(SAVE_SOURCE))
                // Shown right away; the reload below replaces it with the stored row and its id.
                _state.update { it.copy(places = SavedPlacesLogic.distinctPlaces(it.places + place)) }
            }
            val activated = activate(place.lat, place.lon, place.name)
            resetSearch(open = false)
            if (existing == null) load()
            if (activated) _effect.trySend(SavedPlacesEffect.ShowWeather)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun activate(
        lat: Double,
        lon: Double,
        name: String,
    ): Boolean =
        try {
            locationPreferences.saveLocationOverride(lat, lon, name)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _state.update { it.copy(notice = SavedPlacesNotice.SelectFailed) }
            false
        }

    private fun updateSearch(transform: (SavedPlaceSearchState) -> SavedPlaceSearchState) {
        _state.update { it.copy(search = transform(it.search)) }
    }

    private companion object {
        val SEARCH_DEBOUNCE = 400.milliseconds
        const val SAVE_SOURCE = "saved_places_sheet"
        const val SELECT_SOURCE = "saved_places_page"
    }
}
