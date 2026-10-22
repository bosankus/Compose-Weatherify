package bose.ankush.home.domain.nearby

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventDraftTest {
    @Test
    fun startsAtIsTheLocalDateAndTimeInTheGivenZone() {
        val draft = EventDraft(title = "Dinner", dateText = " 2026-10-04 ", timeText = "09:30")

        assertEquals("2026-10-04T09:30:00Z", draft.startsAt(TimeZone.UTC).toString())
    }

    @Test
    fun startsAtNeedsBothAValidDateAndAStrictTime() {
        listOf(
            EventDraft(dateText = "", timeText = "09:30"),
            EventDraft(dateText = "2026-13-01", timeText = "09:30"),
            EventDraft(dateText = "2026-10-04", timeText = ""),
            EventDraft(dateText = "2026-10-04", timeText = "9"),
            EventDraft(dateText = "2026-10-04", timeText = "24:00"),
            EventDraft(dateText = "2026-10-04", timeText = "09:60"),
            EventDraft(dateText = "2026-10-04", timeText = "09:30:00"),
        ).forEach { assertNull(it.startsAt(TimeZone.UTC), it.toString()) }
    }

    @Test
    fun completeNeedsATitleAndAStartTime() {
        assertFalse(
            EventDraft(title = " ", dateText = "2026-10-04", timeText = "09:30").isComplete(
                TimeZone.UTC,
            ),
        )
        assertFalse(EventDraft(title = "Dinner").isComplete(TimeZone.UTC))
        assertTrue(
            EventDraft(
                title = "Dinner",
                dateText = "2026-10-04",
                timeText = "09:30",
            ).isComplete(TimeZone.UTC),
        )
    }
}
