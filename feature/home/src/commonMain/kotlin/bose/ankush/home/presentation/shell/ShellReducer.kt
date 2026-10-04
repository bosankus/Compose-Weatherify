package bose.ankush.home.presentation.shell

import bose.ankush.network.model.SavedLocation
import kotlinx.datetime.LocalDate

internal object ShellReducer {
    fun reduce(
        state: ShellState,
        intent: ShellIntent,
    ): ShellReduce =
        when (intent) {
            is ShellIntent.SelectTab -> selectTab(state, intent.tab)
            ShellIntent.OpenAccount -> ShellReduce(state, ShellEffect.OpenSettings)
            is ShellIntent.LocationUpdated -> locationUpdated(state, intent)
            is ShellIntent.PlaceNameUpdated -> ShellReduce(state.copy(placeName = intent.placeName))
            ShellIntent.OpenCreate -> ShellReduce(state.copy(showCreate = true, createError = null))
            ShellIntent.DismissCreate ->
                ShellReduce(state.copy(showCreate = false, posting = false, createError = null))
            is ShellIntent.TitleChanged -> ShellReduce(state.copy(title = intent.value))
            is ShellIntent.DateChanged -> ShellReduce(state.copy(dateText = intent.value))
            is ShellIntent.TimeChanged -> ShellReduce(state.copy(timeText = intent.value))
            ShellIntent.MarkSavedPlaceLoading -> loadingSavedPlace(state)
            is ShellIntent.SavedPlaceLoaded -> savedPlaceLoaded(state, intent.featured)
            ShellIntent.SavedPlaceFailed -> failedSavedPlace(state)
            ShellIntent.MarkEventsLoading -> loadingEvents(state)
            is ShellIntent.EventsLoaded -> eventsLoaded(state, intent.eventDates, intent.summaries)
            ShellIntent.EventsFailed -> failedEvents(state)
            ShellIntent.MarkAccountLoading -> loadingAccount(state)
            is ShellIntent.AccountLoaded -> accountLoaded(state, intent.photoUrl)
            ShellIntent.AccountFailed -> failedAccount(state)
            is ShellIntent.CreateFailed ->
                ShellReduce(state.copy(posting = false, createError = intent.message))
            ShellIntent.CreateSucceeded -> created(state)
        }

    private fun selectTab(
        state: ShellState,
        tab: ShellTab,
    ): ShellReduce =
        when (tab) {
            ShellTab.SETTINGS -> ShellReduce(state, ShellEffect.OpenSettings)
            else -> ShellReduce(state.copy(tab = tab))
        }

    private fun locationUpdated(
        state: ShellState,
        intent: ShellIntent.LocationUpdated,
    ): ShellReduce =
        ShellReduce(
            state.copy(
                lat = intent.lat,
                lon = intent.lon,
                savedPlace = ShellSectionStatus.Loading,
                featuredPlace = null,
                events = ShellSectionStatus.Loading,
                eventDates = emptySet(),
                eventSummaries = emptyList(),
            ),
        )

    private fun loadingSavedPlace(state: ShellState): ShellReduce =
        ShellReduce(state.copy(savedPlace = ShellSectionStatus.Loading, featuredPlace = null))

    private fun savedPlaceLoaded(
        state: ShellState,
        featured: SavedLocation?,
    ): ShellReduce =
        ShellReduce(
            state.copy(
                featuredPlace = featured,
                savedPlace = if (featured == null) ShellSectionStatus.Empty else ShellSectionStatus.Ready,
            ),
        )

    private fun failedSavedPlace(state: ShellState): ShellReduce =
        ShellReduce(state.copy(savedPlace = ShellSectionStatus.Failed, featuredPlace = null))

    private fun loadingEvents(state: ShellState): ShellReduce =
        ShellReduce(
            state.copy(
                events = ShellSectionStatus.Loading,
                eventDates = emptySet(),
                eventSummaries = emptyList(),
            ),
        )

    private fun eventsLoaded(
        state: ShellState,
        eventDates: Set<LocalDate>,
        summaries: List<ShellEventSummary>,
    ): ShellReduce =
        ShellReduce(
            state.copy(
                eventDates = eventDates,
                eventSummaries = summaries,
                events = if (eventDates.isEmpty()) ShellSectionStatus.Empty else ShellSectionStatus.Ready,
            ),
        )

    private fun failedEvents(state: ShellState): ShellReduce =
        ShellReduce(
            state.copy(
                events = ShellSectionStatus.Failed,
                eventDates = emptySet(),
                eventSummaries = emptyList(),
            ),
        )

    private fun loadingAccount(state: ShellState): ShellReduce =
        ShellReduce(state.copy(accountPhoto = ShellSectionStatus.Loading, photoUrl = null))

    private fun accountLoaded(
        state: ShellState,
        photoUrl: String?,
    ): ShellReduce {
        val url = photoUrl?.takeIf { it.isNotBlank() }
        return ShellReduce(
            state.copy(
                photoUrl = url,
                accountPhoto = if (url == null) ShellSectionStatus.Empty else ShellSectionStatus.Ready,
            ),
        )
    }

    private fun failedAccount(state: ShellState): ShellReduce =
        ShellReduce(state.copy(accountPhoto = ShellSectionStatus.Failed, photoUrl = null))

    private fun created(state: ShellState): ShellReduce =
        ShellReduce(
            state.copy(
                showCreate = false,
                posting = false,
                title = "",
                dateText = "",
                timeText = "",
                createError = null,
            ),
        )
}
