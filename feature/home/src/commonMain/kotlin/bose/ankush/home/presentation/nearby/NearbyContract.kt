package bose.ankush.home.presentation.nearby

import bose.ankush.home.domain.nearby.EventDraft
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.nearby.NearbyEvent
import bose.ankush.home.presentation.util.SectionState
import bose.ankush.home.presentation.util.valueOrNull
import bose.ankush.network.model.SavedLocation
import kotlinx.datetime.LocalDate

/**
 * What is happening around the active location: nearby events, the closest saved place,
 * and the form for adding an event there. The location and its name come from the forecast.
 */
internal data class NearbyState(
    val point: GeoPoint? = null,
    val placeName: String? = null,
    val events: SectionState<List<NearbyEvent>> = SectionState.Loading,
    val featuredPlace: SectionState<SavedLocation?> = SectionState.Loading,
    val composer: EventComposerState = EventComposerState(),
) {
    /** Days to mark on the week strip. */
    val eventDates: Set<LocalDate>
        get() = events.valueOrNull().orEmpty().mapTo(mutableSetOf()) { it.date }

    val canSubmitEvent: Boolean
        get() = point != null && !placeName.isNullOrBlank() && !composer.isSubmitting && composer.draft.isComplete()
}

internal data class EventComposerState(
    val isVisible: Boolean = false,
    val draft: EventDraft = EventDraft(),
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

/** Everything the UI can ask for. */
internal sealed interface NearbyIntent {
    data class LocationChanged(
        val point: GeoPoint,
    ) : NearbyIntent

    data class PlaceNameChanged(
        val name: String,
    ) : NearbyIntent

    data object RetryEvents : NearbyIntent

    data object OpenComposer : NearbyIntent

    data object DismissComposer : NearbyIntent

    data class TitleChanged(
        val value: String,
    ) : NearbyIntent

    data class DateChanged(
        val value: String,
    ) : NearbyIntent

    data class TimeChanged(
        val value: String,
    ) : NearbyIntent

    data object SubmitEvent : NearbyIntent
}

/** State changes, from intents or from finished work. Only [NearbyReducer] applies them. */
internal sealed interface NearbyMutation {
    data class LocationChanged(
        val point: GeoPoint,
    ) : NearbyMutation

    data class PlaceNameChanged(
        val name: String,
    ) : NearbyMutation

    data object EventsLoading : NearbyMutation

    data class EventsLoaded(
        val events: List<NearbyEvent>,
    ) : NearbyMutation

    data object EventsFailed : NearbyMutation

    data object FeaturedPlaceLoading : NearbyMutation

    data class FeaturedPlaceLoaded(
        val place: SavedLocation?,
    ) : NearbyMutation

    data object FeaturedPlaceFailed : NearbyMutation

    data object ComposerOpened : NearbyMutation

    data object ComposerDismissed : NearbyMutation

    data class DraftEdited(
        val title: String? = null,
        val dateText: String? = null,
        val timeText: String? = null,
    ) : NearbyMutation

    data object Submitting : NearbyMutation

    data class SubmitFailed(
        val message: String,
    ) : NearbyMutation

    data object Submitted : NearbyMutation
}
