package bose.ankush.home.presentation.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal fun Long.dayName(): String {
    val localDateTime = Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return localDateTime.dayOfWeek.name
        .lowercase()
        .replaceFirstChar { it.uppercaseChar() }
}

internal fun Long.toFormattedTime(zoneId: String = "Asia/Kolkata"): String {
    val localDateTime = Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.of(zoneId))
    val hour12 =
        when {
            localDateTime.hour == 0 -> 12
            localDateTime.hour > 12 -> localDateTime.hour - 12
            else -> localDateTime.hour
        }
    val minute = localDateTime.minute.toString().padStart(2, '0')
    val amPm = if (localDateTime.hour < 12) "AM" else "PM"
    return "$hour12:$minute $amPm"
}

internal fun getFormattedDateTimeFromEpoch(epoch: Long?): String {
    epoch ?: return "Date & Time is unavailable at this moment"
    val localDateTime = Instant.fromEpochSeconds(epoch).toLocalDateTime(TimeZone.currentSystemDefault())
    val dayAbbrev =
        localDateTime.dayOfWeek.name
            .take(3)
            .lowercase()
            .replaceFirstChar { it.uppercaseChar() }
    val monthAbbrev =
        localDateTime.month.name
            .take(3)
            .lowercase()
            .replaceFirstChar { it.uppercaseChar() }
    val day = localDateTime.day.toString().padStart(2, '0')
    return "$dayAbbrev, $day $monthAbbrev"
}
