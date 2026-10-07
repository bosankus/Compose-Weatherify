package bose.ankush.home.presentation.screen

import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.presentation.util.formatTextCapitalization
import bose.ankush.home.presentation.util.toCelsius
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class CalendarStripDay(
    val label: String,
    val date: String,
    val temp: String,
    val selected: Boolean,
    val min: String = "",
    val max: String = "",
    val description: String = "",
    val icon: String = "",
    val day: LocalDate? = null,
)

/** Week strip inputs grouped so the composable stays under the param-list limit. */
data class WeekCalendarStripModel(
    val days: List<CalendarStripDay>,
    val eventDates: Set<LocalDate> = emptySet(),
    val showWeekShimmer: Boolean = false,
)

/** Live header, chips, and week. Preview data never goes through this. */
data class HomeWeatherContent(
    val temperature: String,
    val place: String,
    val condition: SkyCondition,
    val feel: String,
    val wind: String,
    val uv: String,
    val days: List<CalendarStripDay>,
    val showSmallCards: Boolean = false,
)

/** Ready layout with no invented numbers. Used until Room has a forecast. */
internal fun placeholderHomeWeatherContent(
    place: String = VALUE_PLACEHOLDER,
    condition: SkyCondition = SkyCondition.CLOUDS,
): HomeWeatherContent =
    HomeWeatherContent(
        temperature = VALUE_PLACEHOLDER,
        place = place.ifBlank { VALUE_PLACEHOLDER },
        condition = condition,
        feel = VALUE_PLACEHOLDER,
        wind = VALUE_PLACEHOLDER,
        uv = VALUE_PLACEHOLDER,
        days = emptyList(),
        showSmallCards = false,
    )

/**
 * Leave-by actions. The ViewModel still owns the Remote Config flag, eligibility,
 * and the four analytics events.
 */
data class LeaveByActions(
    val hasJoined: Boolean,
    val hasNotedMisleading: Boolean,
    val onJoin: () -> Unit,
    val onDismiss: () -> Unit,
    val onMisleading: () -> Unit,
)

/**
 * The home screen always uses [Ready]. Missing weather shows placeholder "--" values inside
 * the same layout; there is no separate waiting page.
 */
data class HomeScreenShell(
    val content: HomeWeatherContent,
    val photo: BackgroundPhoto?,
    val leaveBy: LeaveByActions? = null,
    val statusMessage: String? = null,
)

/** OpenWeather `weather.main` to the gradient. The photo query is separate. */
internal fun WeatherForecast.Current.skyCondition(): SkyCondition {
    val main = weather?.firstOrNull()?.main.orEmpty()
    return when (main.lowercase()) {
        "fog", "mist" -> SkyCondition.FOG
        "clear" -> SkyCondition.CLEAR
        "clouds" -> SkyCondition.CLOUDS
        "rain", "drizzle" -> SkyCondition.RAIN
        "thunderstorm", "squall", "tornado" -> SkyCondition.STORM
        "snow" -> SkyCondition.SNOW
        else -> SkyCondition.CLOUDS
    }
}

internal fun WeatherForecast.toHomeWeatherContent(place: String): HomeWeatherContent {
    val now = current
    val condition = now?.skyCondition() ?: SkyCondition.CLOUDS
    return HomeWeatherContent(
        temperature = now?.temp.celsiusLabel(),
        place = place,
        condition = condition,
        feel = now?.feels_like.celsiusLabel(),
        wind = now?.wind_speed?.let { "$it m/s" } ?: MISSING,
        uv = now?.uvi?.let { it.toString() } ?: MISSING,
        days = daily.toCalendarStripDays(),
    )
}

/** Empty when daily is missing, so the live screen does not invent a week. */
internal fun List<WeatherForecast.Daily?>?.toCalendarStripDays(): List<CalendarStripDay> {
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
        val weather = day.weather?.firstOrNull()
        CalendarStripDay(
            label = label,
            date = local.day.toString(),
            temp = temp,
            selected = local.date == today,
            min =
                day.temp
                    ?.min
                    .celsiusLabel()
                    .takeUnless { it == MISSING }
                    .orEmpty(),
            max =
                day.temp
                    ?.max
                    .celsiusLabel()
                    .takeUnless { it == MISSING }
                    .orEmpty(),
            description = weather?.description?.formatTextCapitalization().orEmpty(),
            icon = weather?.icon.orEmpty(),
            day = local.date,
        )
    }
}

private fun Double?.celsiusLabel(): String = this?.let { "${it.toCelsius()}°" } ?: MISSING

internal const val MISSING = VALUE_PLACEHOLDER
private const val DAY_ABBREV = 3
