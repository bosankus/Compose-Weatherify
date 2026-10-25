package bose.ankush.home.presentation.screen

import bose.ankush.home.domain.nearby.NearbyEvent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NearbyEventLinesTest {
    private val oct4 = LocalDate(2026, 10, 4)
    private val oct5 = LocalDate(2026, 10, 5)
    private val week =
        listOf(
            stripDay(oct4, min = "18°", max = "27°", icon = "10d", description = "Light rain"),
            stripDay(oct5, icon = "01d", description = "Clear sky").copy(temp = "25°"),
        )

    @Test
    fun forecastIsLookedUpByTheEventDay() {
        assertEquals(
            EventDayWeather(icon = "10d", range = "18° / 27°", description = "Light rain"),
            week.weatherOn(oct4),
        )
        assertEquals(EventDayWeather(icon = "01d", range = "25°", description = "Clear sky"), week.weatherOn(oct5))
    }

    @Test
    fun noForecastBeyondTheWindow() {
        assertNull(week.weatherOn(LocalDate(2026, 10, 12)))
        assertNull(emptyList<CalendarStripDay>().weatherOn(oct4))
    }

    @Test
    fun aDayWithNoForecastValuesShowsNothing() {
        assertNull(listOf(stripDay(oct4)).weatherOn(oct4))
    }

    @Test
    fun eventLinesCarryTheTimeTheNoteAndTheDayWeather() {
        val lines =
            listOf(
                NearbyEvent("Dinner", LocalDateTime(2026, 10, 4, 18, 5), note = "Table 4"),
                NearbyEvent("Hike", LocalDateTime(2026, 10, 20, 7, 0)),
            ).toNearbyEventLines(week)

        assertEquals(
            NearbyEventLine(
                title = "Dinner",
                whenLabel = "4 Oct",
                timeLabel = "18:05",
                note = "Table 4",
                weather = week.weatherOn(oct4),
            ),
            lines[0],
        )
        assertEquals("07:00", lines[1].timeLabel)
        assertEquals("", lines[1].note)
        assertNull(lines[1].weather)
    }

    @Test
    fun theSelectedDayIsHighlightedInsteadOfToday() {
        val today = stripDay(oct4).copy(selected = true)
        val tomorrow = stripDay(oct5)

        assertTrue(today.isHighlighted(null))
        assertFalse(tomorrow.isHighlighted(null))
        assertFalse(today.isHighlighted(oct5))
        assertTrue(tomorrow.isHighlighted(oct5))
    }

    private fun stripDay(
        day: LocalDate,
        min: String = "",
        max: String = "",
        icon: String = "",
        description: String = "",
    ) = CalendarStripDay(
        label = day.dayOfWeek.name.take(3),
        date = day.day.toString(),
        temp = "",
        selected = false,
        min = min,
        max = max,
        description = description,
        icon = icon,
        day = day,
    )
}
