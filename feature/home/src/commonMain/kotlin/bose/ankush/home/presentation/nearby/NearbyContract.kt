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
 *
 * [selectedDate] is the day picked on the week strip: the list shows only that day and a new
 * event starts on it. [today] is the device date when the events last loaded; anything dated
 * before it is past and stays off the list and the strip. Null means nothing is hidden.
 */
internal data class NearbyState(
    val point: GeoPoint? = null,
    val placeName: String? = null,
    val events: SectionState<List<NearbyEvent>> = SectionState.Loading,
    val featuredPlace: SectionState<SavedLocation?> = SectionState.Loading,
    val composer: EventComposerState = EventComposerState(),
    val selectedDate: LocalDate? = null,
    val today: LocalDate? = null,
) {
    /** Loaded events from [today] on, soonest first. */
    val upcomingEvents: List<NearbyEvent>
        get() =
            events
                .valueOrNull()
                .orEmpty()
                .filter { today == null || it.date >= today }
                .sortedBy { it.startsAt }

    /** What the events list shows: the upcoming events, narrowed to [selectedDate] when set. */
    val visibleEvents: List<NearbyEvent>
        get() = upcomingEvents.filter { selectedDate == null || it.date == selectedDate }

    /** Days to mark on the week strip. */
    val eventDates: Set<LocalDate>
        get() = upcomingEvents.mapTo(mutableSetOf()) { it.date }

    /**
     * A missing place name never blocks Save: the event falls back to a "Pinned location"
     * name at the same coordinates.
     */
    val canSubmitEvent: Boolean
        get() = point != null && !composer.isSubmitting && composer.draft.isComplete()
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

    /** Opens the form. A [date] (a tapped day) is selected and filled in; null uses the selected day. */
    data class OpenComposer(
        val date: LocalDate? = null,
    ) : NearbyIntent

    /** Narrows the list to [date]; null shows every upcoming day again. */
    data class SelectDate(
        val date: LocalDate?,
    ) : NearbyIntent

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

    data class NoteChanged(
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
        val today: LocalDate? = null,
    ) : NearbyMutation

    data object EventsFailed : NearbyMutation

    data object FeaturedPlaceLoading : NearbyMutation

    data class FeaturedPlaceLoaded(
        val place: SavedLocation?,
    ) : NearbyMutation

    data object FeaturedPlaceFailed : NearbyMutation

    data class ComposerOpened(
        val date: LocalDate? = null,
    ) : NearbyMutation

    data class DateSelected(
        val date: LocalDate?,
    ) : NearbyMutation

    data object ComposerDismissed : NearbyMutation

    data class DraftEdited(
        val title: String? = null,
        val dateText: String? = null,
        val timeText: String? = null,
        val note: String? = null,
    ) : NearbyMutation

    data object Submitting : NearbyMutation

    data class SubmitFailed(
        val message: String,
    ) : NearbyMutation

    data object Submitted : NearbyMutation
}
