package bose.ankush.home.presentation.nearby

import bose.ankush.home.domain.nearby.EventDraft
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.nearby.NearbyEvent
import bose.ankush.home.presentation.util.SectionState
import bose.ankush.network.model.SavedLocation
import kotlinx.datetime.LocalDate
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
                events = SectionState.Loaded(listOf(NearbyEvent("Dinner", day))),
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
                NearbyMutation.EventsLoaded(listOf(NearbyEvent("Dinner", day))),
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
    fun saveNeedsAPlaceName() {
        val state =
            NearbyState(
                point = GeoPoint(1.0, 2.0),
                composer = EventComposerState(draft = EventDraft("Dinner", "2026-10-04", "09:30")),
            )

        assertFalse(state.canSubmitEvent)
        assertNull(state.placeName)
    }
}
