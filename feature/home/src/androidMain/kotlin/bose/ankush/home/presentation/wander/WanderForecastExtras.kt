package bose.ankush.home.presentation.wander

import bose.ankush.home.domain.model.WeatherCondition
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.presentation.util.toCelsius
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant

/** Direction of the short temperature trend line. */
enum class WanderTrendDirection {
    COOLING,
    WARMING,
    STEADY,
}

/** Compact trend copy shown under the current report. Null fields mean hide. */
data class WanderTemperatureTrendData(
    val line: String,
    val direction: WanderTrendDirection,
)

/**
 * Precomputed rain cells and temperature trend for Wander home.
 * Null values mean the UI must omit that piece entirely (no "--").
 */
data class WanderForecastExtras(
    val rainToday: String? = null,
    val nextRain: String? = null,
    val temperatureTrend: WanderTemperatureTrendData? = null,
)

/** Builds extras from a live forecast. Empty when nothing useful is present. */
internal fun WeatherForecast.toWanderForecastExtras(
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
): WanderForecastExtras {
    val zone = wanderForecastZone(timezoneOffset)
    return WanderForecastExtras(
        rainToday = formatRainToday(daily?.firstOrNull()?.rain),
        nextRain = findNextRainLabel(hourly.orEmpty(), nowEpochSeconds, zone),
        temperatureTrend =
            findTemperatureTrend(
                currentTempKelvin = current?.temp,
                hourly = hourly.orEmpty(),
                nowEpochSeconds = nowEpochSeconds,
                zone = zone,
            ),
    )
}

/** Today's rain in mm, only when greater than zero. */
internal fun formatRainToday(rainMm: Double?): String? {
    val amount = rainMm?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    val decimals = if (amount % 1.0 == 0.0) 0 else 1
    return formatWanderNumber(amount, decimals = decimals, suffix = " mm", placeholder = "")
        .takeIf { it.isNotBlank() }
}

/**
 * First upcoming hourly slot with thunder (2xx), drizzle (3xx), or rain (5xx).
 * Returns a short clock label in [zone], or "Now" when that slot is the current hour.
 */
internal fun findNextRainLabel(
    hourly: List<WeatherForecast.Hourly?>,
    nowEpochSeconds: Long,
    zone: TimeZone,
): String? {
    val slot =
        hourly
            .asSequence()
            .mapNotNull { it }
            .filter { hour -> (hour.dt ?: Long.MIN_VALUE) >= nowEpochSeconds }
            .firstOrNull { hour -> hour.weather.orEmpty().any { condition -> isPrecipCondition(condition) } }
    return slot?.dt?.let { formatNextRainTime(it, nowEpochSeconds, zone) }
}

internal fun isPrecipCondition(condition: WeatherCondition?): Boolean {
    val id = condition?.id ?: return false
    return id in THUNDER_IDS || id in DRIZZLE_IDS || id in RAIN_IDS
}

/** "Now" when the rain hour matches the current local hour; otherwise "4 PM". */
internal fun formatNextRainTime(
    rainEpochSeconds: Long,
    nowEpochSeconds: Long,
    zone: TimeZone,
): String {
    val rainLocal = Instant.fromEpochSeconds(rainEpochSeconds).toLocalDateTime(zone)
    val nowLocal = Instant.fromEpochSeconds(nowEpochSeconds).toLocalDateTime(zone)
    if (rainLocal.date == nowLocal.date && rainLocal.hour == nowLocal.hour) return NOW
    return toHourClock(rainEpochSeconds, zone)
}

/**
 * Compares current temp with the hourly nearest at or after now+3h.
 * Falls back to the last hourly within 6h. Null when data is missing.
 */
internal fun findTemperatureTrend(
    currentTempKelvin: Double?,
    hourly: List<WeatherForecast.Hourly?>,
    nowEpochSeconds: Long,
    zone: TimeZone,
): WanderTemperatureTrendData? {
    val current = currentTempKelvin?.takeIf { it.isFinite() } ?: return null
    val hours =
        hourly
            .mapNotNull { it }
            .mapNotNull { hour ->
                val dt = hour.dt ?: return@mapNotNull null
                val temp = hour.temp?.takeIf { it.isFinite() } ?: return@mapNotNull null
                dt to temp
            }.sortedBy { it.first }

    val threeHours = nowEpochSeconds + THREE_HOURS_SECONDS
    val sixHours = nowEpochSeconds + SIX_HOURS_SECONDS
    val target =
        hours.firstOrNull { it.first >= threeHours }
            ?: hours.lastOrNull { it.first in (nowEpochSeconds + 1)..sixHours }

    return target?.let { (epoch, futureTemp) ->
        val byTime = toHourClock(epoch, zone)
        val futureCelsius = futureTemp.toCelsius()
        val currentCelsius = current.toCelsius()
        val delta = futureTemp - current
        when {
            abs(delta) < STEADY_DELTA_KELVIN ->
                WanderTemperatureTrendData(
                    line = "Steady around $currentCelsius° until $byTime",
                    direction = WanderTrendDirection.STEADY,
                )
            delta < 0 ->
                WanderTemperatureTrendData(
                    line = "Cooling to $futureCelsius° by $byTime",
                    direction = WanderTrendDirection.COOLING,
                )
            else ->
                WanderTemperatureTrendData(
                    line = "Warming to $futureCelsius° by $byTime",
                    direction = WanderTrendDirection.WARMING,
                )
        }
    }
}

/** 12-hour clock without minutes, e.g. "4 PM". */
internal fun toHourClock(
    epochSeconds: Long,
    zone: TimeZone,
): String {
    val local = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(zone)
    val hour12 =
        when {
            local.hour == 0 -> 12
            local.hour > 12 -> local.hour - 12
            else -> local.hour
        }
    val amPm = if (local.hour < 12) "AM" else "PM"
    return "$hour12 $amPm"
}

private const val NOW = "Now"
private const val THREE_HOURS_SECONDS = 3 * 3600L
private const val SIX_HOURS_SECONDS = 6 * 3600L
private const val STEADY_DELTA_KELVIN = 1.0
private val THUNDER_IDS = 200..299
private val DRIZZLE_IDS = 300..399
private val RAIN_IDS = 500..599
