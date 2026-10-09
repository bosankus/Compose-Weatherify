package bose.ankush.home.presentation.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.nearby.NearbyPlace
import bose.ankush.home.domain.usecase.CreateNearbyEvent
import bose.ankush.home.domain.usecase.FindNearestSavedPlace
import bose.ankush.home.domain.usecase.GetNearbyEvents
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI for [NearbyState]. The UI sends [NearbyIntent]s; work results come back as
 * [NearbyMutation]s that [NearbyReducer] applies.
 *
 * Loads are keyed on the location held in state. Each section collects with `collectLatest`,
 * so a new location cancels the in-flight request for the old one, and the use cases'
 * `withContext` drops its result. No request tokens or stale-location checks are needed.
 */
internal class NearbyViewModel(
    private val getNearbyEvents: GetNearbyEvents,
    private val findNearestSavedPlace: FindNearestSavedPlace,
    private val createNearbyEvent: CreateNearbyEvent,
) : ViewModel() {
    private val _state = MutableStateFlow(NearbyState())
    val state: StateFlow<NearbyState> = _state.asStateFlow()

    /** Re-runs the events request for the current location: retry, and after adding an event. */
    private val eventsReloads = Channel<Unit>(Channel.CONFLATED)

    init {
        val points: Flow<GeoPoint> = _state.mapNotNull { it.point }.distinctUntilChanged()
        viewModelScope.launch {
            merge(points, eventsReloads.receiveAsFlow().mapNotNull { _state.value.point })
                .collectLatest { point -> loadEvents(point) }
        }
        viewModelScope.launch {
            points.collectLatest { point -> loadFeaturedPlace(point) }
        }
    }

    fun onIntent(intent: NearbyIntent) {
        when (intent) {
            is NearbyIntent.LocationChanged ->
                if (intent.point != _state.value.point) mutate(NearbyMutation.LocationChanged(intent.point))

            is NearbyIntent.PlaceNameChanged -> mutate(NearbyMutation.PlaceNameChanged(intent.name))
            NearbyIntent.RetryEvents -> eventsReloads.trySend(Unit)
            NearbyIntent.OpenComposer -> mutate(NearbyMutation.ComposerOpened)
            NearbyIntent.DismissComposer -> mutate(NearbyMutation.ComposerDismissed)
            is NearbyIntent.TitleChanged -> mutate(NearbyMutation.DraftEdited(title = intent.value))
            is NearbyIntent.DateChanged -> mutate(NearbyMutation.DraftEdited(dateText = intent.value))
            is NearbyIntent.TimeChanged -> mutate(NearbyMutation.DraftEdited(timeText = intent.value))
            NearbyIntent.SubmitEvent -> submitEvent()
        }
    }

    private suspend fun loadEvents(point: GeoPoint) {
        mutate(NearbyMutation.EventsLoading)
        getNearbyEvents(point).fold(
            onSuccess = { mutate(NearbyMutation.EventsLoaded(it)) },
            onFailure = { mutate(NearbyMutation.EventsFailed) },
        )
    }

    private suspend fun loadFeaturedPlace(point: GeoPoint) {
        mutate(NearbyMutation.FeaturedPlaceLoading)
        findNearestSavedPlace(point).fold(
            onSuccess = { mutate(NearbyMutation.FeaturedPlaceLoaded(it)) },
            onFailure = { mutate(NearbyMutation.FeaturedPlaceFailed) },
        )
    }

    /**
     * Not canceled by a location change or by closing the form: the POST may already have
     * reached the server, so its outcome is always applied.
     */
    private fun submitEvent() {
        val current = _state.value
        val point = current.point
        val name = current.placeName
        if (!current.canSubmitEvent || point == null || name.isNullOrBlank()) return
        mutate(NearbyMutation.Submitting)
        viewModelScope.launch {
            createNearbyEvent(NearbyPlace(name, point), current.composer.draft).fold(
                onSuccess = {
                    mutate(NearbyMutation.Submitted)
                    eventsReloads.trySend(Unit)
                },
                onFailure = { error ->
                    mutate(
                        NearbyMutation.SubmitFailed(
                            error.message?.takeIf { it.isNotBlank() }
                                ?: SAVE_FAILED,
                        ),
                    )
                },
            )
        }
    }

    /** Atomic read-reduce-write, safe from any thread. */
    private fun mutate(mutation: NearbyMutation) {
        _state.update { NearbyReducer.reduce(it, mutation) }
    }

    private companion object {
        const val SAVE_FAILED = "Couldn't save the event."
    }
}
