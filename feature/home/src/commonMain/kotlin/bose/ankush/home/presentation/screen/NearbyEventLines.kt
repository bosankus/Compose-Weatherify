package bose.ankush.home.presentation.screen

import bose.ankush.home.domain.nearby.NearbyEvent
import bose.ankush.home.presentation.nearby.toDayMonthLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** The daily forecast for the day an event falls on: icon code, range, and condition. */
data class EventDayWeather(
    val icon: String,
    val range: String,
    val description: String,
)

/**
 * One row of the nearby events list. [timeLabel] is the local start (`HH:mm`), [note] the
 * optional text, and [weather] that day's forecast, or null beyond the forecast window.
 */
data class NearbyEventLine(
    val title: String,
    val whenLabel: String,
    val timeLabel: String = "",
    val note: String = "",
    val weather: EventDayWeather? = null,
)

/** The forecast day for [date] from the daily forecast; null when the forecast does not reach it. */
internal fun List<CalendarStripDay>.weatherOn(date: LocalDate): EventDayWeather? =
    firstOrNull { it.day == date }?.let { day ->
        EventDayWeather(icon = day.icon, range = day.rangeLabel(), description = day.description)
            .takeIf { it.icon.isNotBlank() || it.range.isNotBlank() || it.description.isNotBlank() }
    }

/** Rows for [events], each with its start time, note, and the forecast for its day. */
internal fun List<NearbyEvent>.toNearbyEventLines(days: List<CalendarStripDay>): List<NearbyEventLine> =
    map { event ->
        NearbyEventLine(
            title = event.title,
            whenLabel = event.date.toDayMonthLabel(),
            timeLabel = event.startsAt.time.toHourMinuteLabel(),
            note = event.note.orEmpty(),
            weather = days.weatherOn(event.date),
        )
    }

/** "6 / 14°" style range for a strip day, falling back to the single day temperature. */
internal fun CalendarStripDay.rangeLabel(): String =
    when {
        min.isNotEmpty() && max.isNotEmpty() -> "$min / $max"
        min.isNotEmpty() -> min
        max.isNotEmpty() -> max
        else -> temp
    }

/**
 * Whether the strip highlights this day: the selected day when one is picked, otherwise
 * today.
 */
internal fun CalendarStripDay.isHighlighted(selectedDate: LocalDate?): Boolean =
    if (selectedDate != null) day == selectedDate else selected

/** `HH:mm`, 24-hour, zero-padded. */
internal fun LocalTime.toHourMinuteLabel(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
