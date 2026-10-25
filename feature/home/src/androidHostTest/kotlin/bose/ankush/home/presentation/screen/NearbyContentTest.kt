package bose.ankush.home.presentation.screen

import bose.ankush.home.domain.nearby.NearbyEvent
import bose.ankush.home.presentation.nearby.NearbyIntent
import bose.ankush.home.presentation.nearby.NearbyState
import bose.ankush.home.presentation.util.SectionState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NearbyContentTest {
    @Test
    fun aFailedLoadKeepsTheEventsBlockForShimmerAndRetry() {
        val sent = mutableListOf<NearbyIntent>()
        val content =
            NearbyState(events = SectionState.Failed)
                .toNearbyContent(photoUrl = null, onOpenAccount = {}, onIntent = { sent += it })

        assertTrue(content.eventsFailed)
        assertTrue(content.showEventsBlock)
        assertTrue(content.showBlocks)
        content.onRetryEvents()
        assertEquals(listOf<NearbyIntent>(NearbyIntent.RetryEvents), sent)
    }

    @Test
    fun loadingShowsTheBlockAndAnEmptyLoadHidesIt() {
        val loading = NearbyState().toNearbyContent(photoUrl = null, onOpenAccount = {})
        assertTrue(loading.showEventsBlock)

        val empty = NearbyState(events = SectionState.Loaded(emptyList())).toNearbyContent(null, {})
        assertFalse(empty.showEventsBlock)
        assertFalse(empty.showBlocks)
    }

    @Test
    fun aSelectedDayShowsOnlyItsEventsWithTheirWeatherAndCanBeCleared() {
        val day = LocalDate(2026, 10, 5)
        val sent = mutableListOf<NearbyIntent>()
        val state =
            NearbyState(
                events =
                    SectionState.Loaded(
                        listOf(
                            NearbyEvent("Dinner", LocalDateTime(2026, 10, 4, 18, 30)),
                            NearbyEvent("Walk", LocalDateTime(2026, 10, 5, 8, 0)),
                        ),
                    ),
                selectedDate = day,
            )
        val days =
            listOf(CalendarStripDay(label = "Mon", date = "5", temp = "24°", selected = false, icon = "01d", day = day))

        val content = state.toNearbyContent(photoUrl = null, onOpenAccount = {}, days = days, onIntent = { sent += it })

        assertEquals(listOf("Walk"), content.events.map { it.title })
        assertEquals("08:00", content.events.single().timeLabel)
        assertEquals(
            "24°",
            content.events
                .single()
                .weather
                ?.range,
        )
        assertEquals(day, content.selectedDate)
        content.onShowAllDays()
        assertEquals(listOf<NearbyIntent>(NearbyIntent.SelectDate(null)), sent)
    }

    @Test
    fun aSelectedDayWithNoEventsStillShowsTheBlock() {
        val content =
            NearbyState(events = SectionState.Loaded(emptyList()), selectedDate = LocalDate(2026, 10, 9))
                .toNearbyContent(null, {})

        assertTrue(content.showEventsBlock)
    }
}
