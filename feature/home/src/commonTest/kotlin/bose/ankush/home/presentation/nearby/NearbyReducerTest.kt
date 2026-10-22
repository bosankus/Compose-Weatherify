package bose.ankush.home.presentation.nearby

import bose.ankush.home.domain.nearby.EventDraft
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.nearby.NearbyEvent
import bose.ankush.home.presentation.util.SectionState
import bose.ankush.network.model.SavedLocation
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NearbyReducerTest {
    private val day = LocalDate(2026, 10, 4)

    @Test
    fun aNewLocationStartsBothSectionsOver() {
        val start =
            NearbyState(
                point = GeoPoint(1.0, 2.0),
                events = SectionState.Loaded(listOf(NearbyEvent("Dinner", day.atTime(18, 30)))),
                featuredPlace = SectionState.Loaded(SavedLocation(name = "Near")),
            )

        val reduced =
            NearbyReducer.reduce(start, NearbyMutation.LocationChanged(GeoPoint(3.0, 4.0)))

        assertEquals(GeoPoint(3.0, 4.0), reduced.point)
        assertEquals(SectionState.Loading, reduced.events)
        assertEquals(SectionState.Loading, reduced.featuredPlace)
        assertTrue(reduced.eventDates.isEmpty())
    }

    @Test
    fun sectionsKeepLoadingEmptyAndFailedDistinct() {
        val empty = NearbyReducer.reduce(NearbyState(), NearbyMutation.EventsLoaded(emptyList()))
        assertEquals(SectionState.Loaded(emptyList()), empty.events)

        val failed = NearbyReducer.reduce(NearbyState(), NearbyMutation.EventsFailed)
        assertEquals(SectionState.Failed, failed.events)

        val ready =
            NearbyReducer.reduce(
                NearbyState(),
                NearbyMutation.EventsLoaded(listOf(NearbyEvent("Dinner", day.atTime(18, 30)))),
            )
        assertEquals(setOf(day), ready.eventDates)

        val noPlace = NearbyReducer.reduce(NearbyState(), NearbyMutation.FeaturedPlaceLoaded(null))
        assertEquals(SectionState.Loaded(null), noPlace.featuredPlace)

        val placeFailed = NearbyReducer.reduce(NearbyState(), NearbyMutation.FeaturedPlaceFailed)
        assertEquals(SectionState.Failed, placeFailed.featuredPlace)
    }

    @Test
    fun draftEditsOnlyTouchTheirOwnField() {
        val withTitle =
            NearbyReducer.reduce(NearbyState(), NearbyMutation.DraftEdited(title = "Dinner"))
        val withDate =
            NearbyReducer.reduce(withTitle, NearbyMutation.DraftEdited(dateText = "2026-10-04"))

        assertEquals(EventDraft(title = "Dinner", dateText = "2026-10-04"), withDate.composer.draft)
    }

    @Test
    fun submittingDisablesSaveAndAFailureKeepsTheDraft() {
        val ready =
            NearbyState(
                point = GeoPoint(1.0, 2.0),
                placeName = "Harbor",
                composer =
                    EventComposerState(
                        isVisible = true,
                        draft = EventDraft("Dinner", "2026-10-04", "09:30"),
                    ),
            )
        assertTrue(ready.canSubmitEvent)

        val submitting = NearbyReducer.reduce(ready, NearbyMutation.Submitting)
        assertFalse(submitting.canSubmitEvent)

        val failed = NearbyReducer.reduce(submitting, NearbyMutation.SubmitFailed("offline"))
        assertEquals("offline", failed.composer.error)
        assertEquals(ready.composer.draft, failed.composer.draft)
        assertTrue(failed.canSubmitEvent)
    }

    @Test
    fun aSavedEventClosesAndClearsTheForm() {
        val open =
            NearbyState(
                composer =
                    EventComposerState(
                        isVisible = true,
                        draft = EventDraft("Dinner"),
                    ),
            )

        val saved = NearbyReducer.reduce(open, NearbyMutation.Submitted)

        assertEquals(EventComposerState(), saved.composer)
    }

    @Test
    fun aMissingPlaceNameDoesNotBlockSave() {
        val state =
            NearbyState(
                point = GeoPoint(1.0, 2.0),
                composer = EventComposerState(draft = EventDraft("Dinner", "2026-10-04", "09:30")),
            )

        assertTrue(state.canSubmitEvent)
        assertNull(state.placeName)
        assertFalse(state.copy(point = null).canSubmitEvent)
    }

    @Test
    fun aTappedDayOpensTheFormWithThatDateAndSelectsIt() {
        val opened = NearbyReducer.reduce(NearbyState(), NearbyMutation.ComposerOpened(day))

        assertTrue(opened.composer.isVisible)
        assertEquals("2026-10-04", opened.composer.draft.dateText)
        assertEquals(day, opened.selectedDate)
    }

    @Test
    fun plusUsesTheSelectedDay() {
        val selected = NearbyReducer.reduce(NearbyState(), NearbyMutation.DateSelected(day))

        val opened = NearbyReducer.reduce(selected, NearbyMutation.ComposerOpened(null))

        assertEquals("2026-10-04", opened.composer.draft.dateText)
        assertEquals(day, opened.selectedDate)
    }

    @Test
    fun plusWithNoSelectionKeepsTheDraftDate() {
        val start = NearbyState(composer = EventComposerState(draft = EventDraft(dateText = "2026-10-09")))

        val opened = NearbyReducer.reduce(start, NearbyMutation.ComposerOpened(null))

        assertEquals("2026-10-09", opened.composer.draft.dateText)
        assertNull(opened.selectedDate)
    }

    @Test
    fun selectingADayFiltersTheListToThatDay() {
        val other = day.plus(1, DateTimeUnit.DAY)
        val loaded =
            NearbyReducer.reduce(
                NearbyState(),
                NearbyMutation.EventsLoaded(
                    listOf(NearbyEvent("Walk", other.atTime(8, 0)), NearbyEvent("Dinner", day.atTime(18, 30))),
                    today = day,
                ),
            )

        val selected = NearbyReducer.reduce(loaded, NearbyMutation.DateSelected(other))
        assertEquals(listOf("Walk"), selected.visibleEvents.map { it.title })
        // The strip still marks every upcoming day.
        assertEquals(setOf(day, other), selected.eventDates)

        val cleared = NearbyReducer.reduce(selected, NearbyMutation.DateSelected(null))
        assertEquals(listOf("Dinner", "Walk"), cleared.visibleEvents.map { it.title })
    }

    @Test
    fun pastEventsAreHiddenFromTheListAndTheStrip() {
        val yesterday = day.minus(1, DateTimeUnit.DAY)
        val loaded =
            NearbyReducer.reduce(
                NearbyState(),
                NearbyMutation.EventsLoaded(
                    listOf(
                        NearbyEvent("Old", yesterday.atTime(23, 59)),
                        NearbyEvent("Later", day.atTime(20, 0)),
                        NearbyEvent("Earlier", day.atTime(7, 0)),
                    ),
                    today = day,
                ),
            )

        assertEquals(listOf("Earlier", "Later"), loaded.visibleEvents.map { it.title })
        assertEquals(setOf(day), loaded.eventDates)
        assertEquals(3, (loaded.events as SectionState.Loaded).value.size)
    }

    @Test
    fun noteEditsOnlyTouchTheNote() {
        val start = NearbyState(composer = EventComposerState(draft = EventDraft(title = "Dinner")))

        val reduced = NearbyReducer.reduce(start, NearbyMutation.DraftEdited(note = "Table 4"))

        assertEquals(EventDraft(title = "Dinner", note = "Table 4"), reduced.composer.draft)
    }
}
