package bose.ankush.home.presentation.wander

import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Zone for a forecast's own clock. [offsetSeconds] is OpenWeather's `timezone_offset`
 * (e.g. -25200 for Mountain View in PDT). Null falls back to the device zone.
 */
internal fun forecastTimeZone(offsetSeconds: Int?): TimeZone =
    offsetSeconds?.let { FixedOffsetTimeZone(UtcOffset(seconds = it)) } ?: TimeZone.currentSystemDefault()

/**
 * Zone the Wander home formats sunrise, sunset, and the observation time in: the forecast
 * location's [timezoneOffset] (seconds), or the device zone when the forecast has none.
 */
internal fun wanderForecastZone(timezoneOffset: Int?): TimeZone = forecastTimeZone(offsetSeconds = timezoneOffset)

/** 12-hour clock in [zone], e.g. "7:06 AM". */
internal fun Long.toClock(zone: TimeZone): String {
    val local = Instant.fromEpochSeconds(this).toLocalDateTime(zone)
    val hour12 =
        when {
            local.hour == 0 -> 12
            local.hour > 12 -> local.hour - 12
            else -> local.hour
        }
    val minute = local.minute.toString().padStart(2, '0')
    val amPm = if (local.hour < 12) "AM" else "PM"
    return "$hour12:$minute $amPm"
}

/** Observation label in [zone], e.g. "Sun, 04 Oct, 8:22 PM". */
internal fun observedLabel(
    epochSeconds: Long,
    zone: TimeZone,
): String {
    val local = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(zone)
    val day = local.dayOfWeek.name.abbreviated()
    val month = local.month.name.abbreviated()
    val date = local.day.toString().padStart(2, '0')
    return "$day, $date $month, ${epochSeconds.toClock(zone)}"
}

/** Daily summary first, then the current description, then the condition line. */
internal fun wanderHeaderLine(
    summary: String?,
    description: String?,
    fallback: String,
): String =
    summary?.trim()?.takeIf { it.isNotEmpty() }
        ?: description?.trim()?.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.uppercaseChar() }
        ?: fallback

private fun String.abbreviated(): String =
    take(ABBREV)
        .lowercase()
        .replaceFirstChar { it.uppercaseChar() }

private const val ABBREV = 3
