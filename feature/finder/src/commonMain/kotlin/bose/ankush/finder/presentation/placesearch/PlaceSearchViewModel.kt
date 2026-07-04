package bose.ankush.finder.presentation.placesearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.finder.domain.usecase.SearchPlacesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val MIN_QUERY_LENGTH = 2

@OptIn(FlowPreview::class)
internal class PlaceSearchViewModel(
    private val searchPlacesUseCase: SearchPlacesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(PlaceSearchState())
    val state: StateFlow<PlaceSearchState> = _state.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _queryFlow
                .debounce(500.milliseconds)
                .filter { it.length >= MIN_QUERY_LENGTH }
                .distinctUntilChanged()
                .collectLatest { query -> fetchPlaceSuggestions(query) }
        }
    }

    fun processIntent(intent: PlaceSearchIntent) {
        when (intent) {
            is PlaceSearchIntent.QueryChanged -> onQueryChanged(intent.query)
            is PlaceSearchIntent.Clear -> clear()
        }
    }

    private fun onQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query, error = null) }
        _queryFlow.value = query
        if (query.length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(results = emptyList(), isLoading = false) }
        }
    }

    private fun clear() {
        _state.value = PlaceSearchState()
        _queryFlow.value = ""
    }

    private suspend fun fetchPlaceSuggestions(query: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        searchPlacesUseCase(query).fold(
            onSuccess = { suggestions ->
                _state.update { it.copy(isLoading = false, results = suggestions) }
            },
            onFailure = { e ->
                if (e !is CancellationException) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Unable to fetch places. Please try again.",
                        )
                    }
                }
            },
        )
    }
}
