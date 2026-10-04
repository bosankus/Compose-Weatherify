package bose.ankush.home.presentation.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.network.model.CreatePlaceEventRequest
import bose.ankush.network.repository.AccountRepository
import bose.ankush.network.repository.LocationRepository
import bose.ankush.network.repository.PlaceEventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ShellViewModel(
    private val locationRepository: LocationRepository,
    private val placeEventRepository: PlaceEventRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ShellState())
    val state: StateFlow<ShellState> = _state.asStateFlow()

    private val _effect = Channel<ShellEffect>(Channel.BUFFERED)
    val effect: Flow<ShellEffect> = _effect.receiveAsFlow()

    private var sideJob: Job? = null

    init {
        viewModelScope.launch {
            val photoUrl =
                accountRepository
                    .getAccount()
                    .getOrNull()
                    ?.photoUrl
                    ?.takeIf { it.isNotBlank() }
            dispatch(ShellIntent.AccountLoaded(photoUrl))
        }
    }

    fun onIntent(intent: ShellIntent) {
        when (intent) {
            is ShellIntent.LocationUpdated -> {
                val changed = _state.value.lat != intent.lat || _state.value.lon != intent.lon
                if (changed) {
                    dispatch(intent)
                    refresh(intent.lat, intent.lon)
                }
            }

            else -> dispatch(intent)
        }
    }

    fun submitCreate() {
        val request = createRequestOrNull(_state.value)
        if (request != null) {
            _state.update { it.copy(posting = true, createError = null) }
            viewModelScope.launch {
                val result = placeEventRepository.createPlaceEvent(request)
                result.fold(
                    onSuccess = {
                        dispatch(ShellIntent.CreateSucceeded)
                        refresh(request.lat, request.lon)
                    },
                    onFailure = { error ->
                        val message = error.message?.takeIf { it.isNotBlank() } ?: SAVE_FAILED
                        dispatch(ShellIntent.CreateFailed(message))
                    },
                )
            }
        }
    }

    private fun createRequestOrNull(state: ShellState): CreatePlaceEventRequest? {
        if (state.posting || !canSubmitEvent(state)) return null
        return CreatePlaceEventRequest(
            placeName = checkNotNull(state.placeName),
            lat = checkNotNull(state.lat),
            lon = checkNotNull(state.lon),
            title = state.title.trim(),
            startsAt = checkNotNull(startsAtOrNull(state.dateText, state.timeText)),
            note = null,
        )
    }

    private fun refresh(
        lat: Double,
        lon: Double,
    ) {
        sideJob?.cancel()
        sideJob =
            viewModelScope.launch {
                val places = locationRepository.getSavedLocations()
                val events = placeEventRepository.getPlaceEvents(lat, lon)
                if (_state.value.lat != lat || _state.value.lon != lon) return@launch
                val featured = places.getOrNull()?.let { closestSavedPlace(it, lat, lon) }
                val dates = events.getOrNull()?.let { eventDatesFrom(it) } ?: emptySet()
                dispatch(ShellIntent.SideDataLoaded(featured, dates))
            }
    }

    private fun dispatch(intent: ShellIntent) {
        val reduced = ShellReducer.reduce(_state.value, intent)
        _state.value = reduced.state
        reduced.effect?.let { _effect.trySend(it) }
    }

    private companion object {
        const val SAVE_FAILED = "Couldn't save the event."
    }
}
