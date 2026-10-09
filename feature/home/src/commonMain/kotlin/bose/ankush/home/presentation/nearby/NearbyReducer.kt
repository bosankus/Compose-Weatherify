package bose.ankush.home.presentation.nearby

import bose.ankush.home.domain.nearby.EventDraft
import bose.ankush.home.presentation.util.SectionState

/** Pure: the only place [NearbyState] changes. */
internal object NearbyReducer {
    fun reduce(
        state: NearbyState,
        mutation: NearbyMutation,
    ): NearbyState =
        when (mutation) {
            // A new location starts both sections over, so nothing from the old one lingers.
            is NearbyMutation.LocationChanged ->
                state.copy(
                    point = mutation.point,
                    events = SectionState.Loading,
                    featuredPlace = SectionState.Loading,
                )

            is NearbyMutation.PlaceNameChanged -> state.copy(placeName = mutation.name)
            NearbyMutation.EventsLoading -> state.copy(events = SectionState.Loading)
            is NearbyMutation.EventsLoaded -> state.copy(events = SectionState.Loaded(mutation.events))
            NearbyMutation.EventsFailed -> state.copy(events = SectionState.Failed)
            NearbyMutation.FeaturedPlaceLoading -> state.copy(featuredPlace = SectionState.Loading)
            is NearbyMutation.FeaturedPlaceLoaded ->
                state.copy(
                    featuredPlace =
                        SectionState.Loaded(
                            mutation.place,
                        ),
                )

            NearbyMutation.FeaturedPlaceFailed -> state.copy(featuredPlace = SectionState.Failed)
            NearbyMutation.ComposerOpened ->
                state.withComposer {
                    it.copy(
                        isVisible = true,
                        error = null,
                    )
                }

            NearbyMutation.ComposerDismissed ->
                state.withComposer {
                    it.copy(
                        isVisible = false,
                        isSubmitting = false,
                        error = null,
                    )
                }

            is NearbyMutation.DraftEdited ->
                state.withComposer {
                    it.copy(
                        draft =
                            it.draft.edit(
                                mutation,
                            ),
                    )
                }

            NearbyMutation.Submitting ->
                state.withComposer {
                    it.copy(
                        isSubmitting = true,
                        error = null,
                    )
                }

            is NearbyMutation.SubmitFailed ->
                state.withComposer { it.copy(isSubmitting = false, error = mutation.message) }

            NearbyMutation.Submitted -> state.copy(composer = EventComposerState())
        }

    private inline fun NearbyState.withComposer(change: (EventComposerState) -> EventComposerState): NearbyState =
        copy(composer = change(composer))

    private fun EventDraft.edit(mutation: NearbyMutation.DraftEdited): EventDraft =
        copy(
            title = mutation.title ?: title,
            dateText = mutation.dateText ?: dateText,
            timeText = mutation.timeText ?: timeText,
        )
}
