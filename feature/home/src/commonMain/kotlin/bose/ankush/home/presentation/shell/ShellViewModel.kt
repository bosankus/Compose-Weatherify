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

    private var savedJob: Job? = null
    private var eventsJob: Job? = null
    private var accountJob: Job? = null
    private var savedToken = 0
    private var eventsToken = 0
    private var accountToken = 0

    /** GET /account. The signed photo URL is kept in memory only. */
    fun refreshAccount() {
        accountJob?.cancel()
        val token = ++accountToken
        dispatch(ShellIntent.MarkAccountLoading)
        accountJob =
            viewModelScope.launch {
                val result = accountRepository.getAccount()
                if (token != accountToken) return@launch
                result.fold(
                    onSuccess = { account -> dispatch(ShellIntent.AccountLoaded(account.photoUrl)) },
                    onFailure = { dispatch(ShellIntent.AccountFailed) },
                )
            }
    }

    fun retrySavedPlace() {
        val lat = _state.value.lat
        val lon = _state.value.lon
        if (lat == null || lon == null) {
            _effect.trySend(ShellEffect.ReloadForecast)
        } else {
            refreshSaved(lat, lon)
        }
    }

    fun retryEvents() {
        val lat = _state.value.lat
        val lon = _state.value.lon
        if (lat == null || lon == null) {
            _effect.trySend(ShellEffect.ReloadForecast)
        } else {
            refreshEvents(lat, lon)
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
        refreshSaved(lat, lon)
        refreshEvents(lat, lon)
    }

    private fun refreshSaved(
        lat: Double,
        lon: Double,
    ) {
        savedJob?.cancel()
        val token = ++savedToken
        dispatch(ShellIntent.MarkSavedPlaceLoading)
        savedJob =
            viewModelScope.launch {
                val result = locationRepository.getSavedLocations()
                if (token != savedToken || _state.value.lat != lat || _state.value.lon != lon) return@launch
                result.fold(
                    onSuccess = { places ->
                        dispatch(ShellIntent.SavedPlaceLoaded(closestSavedPlace(places, lat, lon)))
                    },
                    onFailure = { dispatch(ShellIntent.SavedPlaceFailed) },
                )
            }
    }

    private fun refreshEvents(
        lat: Double,
        lon: Double,
    ) {
        eventsJob?.cancel()
        val token = ++eventsToken
        dispatch(ShellIntent.MarkEventsLoading)
        eventsJob =
            viewModelScope.launch {
                val result = placeEventRepository.getPlaceEvents(lat, lon)
                if (token != eventsToken || _state.value.lat != lat || _state.value.lon != lon) return@launch
                result.fold(
                    onSuccess = { events -> dispatch(ShellIntent.EventsLoaded(eventDatesFrom(events))) },
                    onFailure = { dispatch(ShellIntent.EventsFailed) },
                )
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
