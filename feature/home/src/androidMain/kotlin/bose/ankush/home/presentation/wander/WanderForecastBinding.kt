package bose.ankush.home.presentation.wander

import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.presentation.util.toCelsius
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class WanderCalendarDay(
    val label: String,
    val date: String,
    val temp: String,
    val selected: Boolean,
)

/** Live header, chips, and week. Preview data never goes through this. */
data class WanderHomeContent(
    val temperature: String,
    val place: String,
    val condition: WanderCondition,
    val feel: String,
    val wind: String,
    val uv: String,
    val days: List<WanderCalendarDay>,
    val showSmallCards: Boolean = false,
)

/**
 * Leave-by actions. The ViewModel still owns the Remote Config flag, eligibility,
 * and the four analytics events.
 */
data class WanderLeaveBy(
    val hasJoined: Boolean,
    val hasNotedMisleading: Boolean,
    val onJoin: () -> Unit,
    val onDismiss: () -> Unit,
    val onMisleading: () -> Unit,
)

/** What the shell shows. Ready is live forecast data. Waiting has no mock numbers. */
sealed interface WanderShell {
    val leaveBy: WanderLeaveBy?

    data class Ready(
        val content: WanderHomeContent,
        val photo: WanderFogPhoto?,
        override val leaveBy: WanderLeaveBy? = null,
    ) : WanderShell

    data class Waiting(
        val loading: Boolean,
        val statusMessage: String?,
        override val leaveBy: WanderLeaveBy? = null,
    ) : WanderShell
}

/** OpenWeather `weather.main` to the gradient. The photo query is separate. */
internal fun WeatherForecast.Current.wanderCondition(): WanderCondition {
    val main = weather?.firstOrNull()?.main.orEmpty()
    return when (main.lowercase()) {
        "fog", "mist" -> WanderCondition.FOG
        "clear" -> WanderCondition.CLEAR
        "clouds" -> WanderCondition.CLOUDS
        "rain", "drizzle" -> WanderCondition.RAIN
        "thunderstorm", "squall", "tornado" -> WanderCondition.STORM
        "snow" -> WanderCondition.SNOW
        else -> WanderCondition.CLOUDS
    }
}

internal fun WeatherForecast.toWanderContent(place: String): WanderHomeContent {
    val now = current
    val condition = now?.wanderCondition() ?: WanderCondition.CLOUDS
    return WanderHomeContent(
        temperature = now?.temp.celsiusLabel(),
        place = place,
        condition = condition,
        feel = now?.feels_like.celsiusLabel(),
        wind = now?.wind_speed?.let { "$it m/s" } ?: MISSING,
        uv = now?.uvi?.let { it.toString() } ?: MISSING,
        days = daily.toWanderDays(),
    )
}

/** Empty when daily is missing, so the live screen does not invent a week. */
internal fun List<WeatherForecast.Daily?>?.toWanderDays(): List<WanderCalendarDay> {
    if (this.isNullOrEmpty()) return emptyList()
    val today =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    return mapNotNull { day ->
        val epoch = day?.dt ?: return@mapNotNull null
        val local = Instant.fromEpochSeconds(epoch).toLocalDateTime(TimeZone.currentSystemDefault())
        val label =
            local.dayOfWeek.name
                .take(DAY_ABBREV)
                .lowercase()
                .replaceFirstChar { it.uppercaseChar() }
        val temp = (day.temp?.day ?: day.temp?.max).celsiusLabel().takeUnless { it == MISSING }.orEmpty()
        WanderCalendarDay(
            label = label,
            date = local.day.toString(),
            temp = temp,
            selected = local.date == today,
        )
    }
}

private fun Double?.celsiusLabel(): String = this?.let { "${it.toCelsius()}°" } ?: MISSING

private const val MISSING = "—"
private const val DAY_ABBREV = 3
