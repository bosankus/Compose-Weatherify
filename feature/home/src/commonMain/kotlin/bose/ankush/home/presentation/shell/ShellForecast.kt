package bose.ankush.home.presentation.shell

import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.presentation.util.toCelsius
import bose.ankush.network.model.PlaceEvent
import bose.ankush.network.model.SavedLocation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Instant

internal data class ShellDay(
    val label: String,
    val dayOfMonth: String,
    val date: LocalDate,
    val selected: Boolean,
)

/** Live forecast fields only. Missing values stay as [MISSING], never a made-up number. */
internal data class ShellForecast(
    val temperatureLine: String,
    val temperature: String,
    val place: String,
    val conditionLine: String,
    val feel: String,
    val wind: String,
    val uv: String,
    val days: List<ShellDay>,
)

/**
 * "Closely" was not given a radius. One kilometre is an assumption, the same order of
 * magnitude as the place-events follow-up, not a value from the contract.
 */
internal const val SAVED_PLACE_MATCH_METERS = 1_000.0

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val DAY_ABBREV = 3
private const val DEGREES_IN_HALF_TURN = 180.0
private const val MAX_HOUR = 23
private const val MAX_MINUTE = 59
private const val HOUR_PART = 0
private const val MINUTE_PART = 1
internal const val MISSING = "—"

internal fun WeatherForecast.toShellForecast(
    place: String?,
    today: LocalDate,
): ShellForecast {
    val now = current
    val tempLabel = now?.temp?.let { "${it.toCelsius()}°" }
    val placeLabel = place?.takeIf { it.isNotBlank() }
    val temperatureLine =
        when {
            tempLabel != null && placeLabel != null -> "$tempLabel, $placeLabel"
            tempLabel != null -> tempLabel
            placeLabel != null -> placeLabel
            else -> MISSING
        }
    return ShellForecast(
        temperatureLine = temperatureLine,
        temperature = tempLabel ?: if (placeLabel == null) MISSING else "",
        place = placeLabel.orEmpty(),
        conditionLine = conditionLine(now),
        feel = now?.feels_like?.let { "${it.toCelsius()}°" } ?: MISSING,
        wind = now?.wind_speed?.let { "$it m/s" } ?: MISSING,
        uv = now?.uvi?.let { it.toString() } ?: MISSING,
        days = daily.toShellDays(today),
    )
}

private fun conditionLine(now: WeatherForecast.Current?): String {
    val weather = now?.weather?.firstOrNull()
    val words =
        weather?.description?.takeIf { it.isNotBlank() }
            ?: weather?.main?.takeIf { it.isNotBlank() }
    return words?.let { "It's ${it.lowercase()}" } ?: MISSING
}

private fun List<WeatherForecast.Daily?>?.toShellDays(today: LocalDate): List<ShellDay> {
    if (this.isNullOrEmpty()) return emptyList()
    return mapNotNull { day ->
        val epoch = day?.dt ?: return@mapNotNull null
        val local = Instant.fromEpochSeconds(epoch).toLocalDateTime(TimeZone.currentSystemDefault())
        val label =
            local.dayOfWeek.name
                .take(DAY_ABBREV)
                .lowercase()
                .replaceFirstChar { it.uppercaseChar() }
        ShellDay(
            label = label,
            dayOfMonth = local.day.toString(),
            date = local.date,
            selected = local.date == today,
        )
    }
}

internal fun distanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val startLat = lat1.toRadians()
    val endLat = lat2.toRadians()
    val dLat = (lat2 - lat1).toRadians()
    val dLon = (lon2 - lon1).toRadians()
    val a = sin(dLat / 2).pow(2) + cos(startLat) * cos(endLat) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

/** Nearest saved place within [SAVED_PLACE_MATCH_METERS], or null when none qualifies. */
internal fun closestSavedPlace(
    places: List<SavedLocation>,
    lat: Double,
    lon: Double,
): SavedLocation? =
    places
        .map { place -> place to distanceMeters(lat, lon, place.lat, place.lon) }
        .filter { (_, meters) -> meters <= SAVED_PLACE_MATCH_METERS }
        .minByOrNull { (_, meters) -> meters }
        ?.first

/**
 * Dates to mark. GET /place-events matches within about 1 km, so an empty list
 * means there are no events nearby and nothing is marked.
 */
internal fun eventDatesFrom(events: List<PlaceEvent>): Set<LocalDate> =
    events.mapNotNull { eventLocalDate(it.startsAt) }.toSet()

internal fun eventLocalDate(startsAt: String): LocalDate? =
    try {
        Instant.parse(startsAt).toLocalDateTime(TimeZone.currentSystemDefault()).date
    } catch (_: IllegalArgumentException) {
        null
    }

/** ISO-8601 instant from a user-entered local date and time. Null until both parse. */
internal fun startsAtOrNull(
    dateText: String,
    timeText: String,
): String? {
    val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
    val time = localTimeOrNull(timeText)
    return if (date == null || time == null) {
        null
    } else {
        runCatching {
            LocalDateTime(date, time)
                .toInstant(TimeZone.currentSystemDefault())
                .toString()
        }.getOrNull()
    }
}

private fun localTimeOrNull(timeText: String): LocalTime? {
    val parts = timeText.trim().split(":")
    val hour = parts.getOrNull(HOUR_PART)?.toIntOrNull()
    val minute = parts.getOrNull(MINUTE_PART)?.toIntOrNull()
    return when {
        parts.size != 2 -> null
        hour == null -> null
        minute == null -> null
        hour !in 0..MAX_HOUR -> null
        minute !in 0..MAX_MINUTE -> null
        else -> LocalTime(hour, minute)
    }
}

internal fun canSubmitEvent(state: ShellState): Boolean {
    val hasPlace = !state.placeName.isNullOrBlank() && state.lat != null && state.lon != null
    return state.title.trim().isNotEmpty() && hasPlace && startsAtOrNull(state.dateText, state.timeText) != null
}

private fun Double.toRadians(): Double = this * kotlin.math.PI / DEGREES_IN_HALF_TURN
