package bose.ankush.home.presentation.screen

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
 * Zone the home screen formats sunrise, sunset, and the observation time in: the forecast
 * location's [timezoneOffset] (seconds), or the device zone when the forecast has none.
 */
internal fun forecastZone(timezoneOffset: Int?): TimeZone =
    forecastTimeZone(offsetSeconds = timezoneOffset)

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
internal fun headerSummaryLine(
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

/** Device-zone clock, as before. Home screen sun times use [toClock] with [forecastZone]. */
internal fun Long.toClock(): String = toClock(TimeZone.currentSystemDefault())

internal fun Long.toIssuedLabel(): String {
    val local = Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val month =
        local.month.name
            .take(MONTH_ABBREV)
            .lowercase()
            .replaceFirstChar { it.uppercaseChar() }
    return "$month ${local.day}, ${toClock()}"
}

/**
 * 0 at sunrise and earlier, 1 at sunset and later.
 * Missing or inverted times stay at the sunrise end.
 */
internal fun sunAlongDay(
    sunrise: Long?,
    sunset: Long?,
    nowEpochSeconds: Long,
): Float {
    val rise = sunrise
    val set = sunset
    return when {
        rise == null || set == null || set <= rise -> 0f
        nowEpochSeconds <= rise -> 0f
        nowEpochSeconds >= set -> 1f
        else -> {
            val span = (set - rise).toFloat()
            val elapsed = (nowEpochSeconds - rise).toFloat()
            (elapsed / span).coerceIn(0f, 1f)
        }
    }
}

private const val MONTH_ABBREV = 3
