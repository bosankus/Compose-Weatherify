package bose.ankush.home.domain.nearby

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/**
 * What the user entered in the new-event form. The date (`YYYY-MM-DD`) and time (`HH:mm`)
 * are held as text, filled by the pickers, because POST /place-events needs [startsAt] as an
 * ISO-8601 instant. [note] is optional.
 */
internal data class EventDraft(
    val title: String = "",
    val dateText: String = "",
    val timeText: String = "",
    val note: String = "",
) {
    /** The start instant in [timeZone], or null until both the date and the time parse. */
    fun startsAt(timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant? {
        val date = date()
        val time = time()
        return if (date == null || time == null) {
            null
        } else {
            runCatching { LocalDateTime(date, time).toInstant(timeZone) }.getOrNull()
        }
    }

    fun isComplete(timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean =
        title.isNotBlank() && startsAt(timeZone) != null

    /** The parsed date, or null while [dateText] is empty or malformed. */
    fun date(): LocalDate? = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()

    /** The parsed time, or null while [timeText] is empty or malformed. */
    fun time(): LocalTime? = parseTime(timeText)
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
