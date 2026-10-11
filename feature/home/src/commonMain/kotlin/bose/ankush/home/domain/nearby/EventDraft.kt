package bose.ankush.home.domain.nearby

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/**
 * What the user typed into the new-event form. The date and time are collected as text
 * because POST /place-events needs [startsAt] as an ISO-8601 instant.
 */
internal data class EventDraft(
    val title: String = "",
    val dateText: String = "",
    val timeText: String = "",
) {
    /** The start instant in [timeZone], or null until both the date and the time parse. */
    fun startsAt(timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant? {
        val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
        val time = parseTime(timeText)
        return if (date == null || time == null) {
            null
        } else {
            runCatching { LocalDateTime(date, time).toInstant(timeZone) }.getOrNull()
        }
    }

    fun isComplete(timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean =
        title.isNotBlank() && startsAt(timeZone) != null
}

/** Strict `HH:mm`, 24-hour. */
private fun parseTime(text: String): LocalTime? {
    val parts = text.trim().split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..MAX_HOUR }
    val minute = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..MAX_MINUTE }
    return if (parts.size == 2 && hour != null && minute != null) LocalTime(hour, minute) else null
}

private const val MAX_HOUR = 23
private const val MAX_MINUTE = 59
