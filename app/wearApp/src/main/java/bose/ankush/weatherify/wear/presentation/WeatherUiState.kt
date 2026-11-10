package bose.ankush.weatherify.wear.presentation

internal data class WeatherUiState(
    val location: String,
    val condition: String?,
    /** Short upper-case condition group ("RAIN", "CLEAR") for the dial hub. */
    val conditionShort: String?,
    val icon: WeatherIconType,
    val dayPhase: DayPhase,
    /** Bare degrees ("19°"); the dial has no room for the unit suffix. */
    val temperature: String,
    val feelsLike: String?,
    val highTemp: String?,
    val lowTemp: String?,
    val humidity: String?,
    val wind: String?,
    val uvIndex: String?,
    val uvLevel: String?,
    val today: TodayUiState,
    val hourly: List<HourlyUiState>,
    val daily: List<DailyUiState>,
    val alert: AlertUiState?,
)

/** The "today at a glance" facts behind the details grid. Each is null when the phone's
 * sync didn't include it (e.g. air quality is a paid entitlement). */
internal data class TodayUiState(
    val sunrise: String?,
    val sunset: String?,
    val airQuality: String?,
    val pressure: String?,
    val rain: String?,
    val clouds: String?,
)

/** Coarse time-of-day bucket derived from the synced sunrise/sunset: full daylight, the
 * band around sunrise/sunset, and night. Picks day/night Lottie variants. */
internal enum class DayPhase {
    DAY,
    TWILIGHT,
    NIGHT,
}

internal data class AlertUiState(
    val event: String,
    val description: String,
    val sender: String?,
    val start: String?,
    val end: String?,
)

internal data class HourlyUiState(
    /** "15:00", shown in the hourly dial's centre. */
    val time: String,
    /** "15", shown on the hourly dial's ring. */
    val hourLabel: String,
    val temperature: String,
    val condition: String?,
    val icon: WeatherIconType,
)

internal data class DailyUiState(
    /** "Today", or a short weekday ("Tue"). */
    val day: String,
    val condition: String?,
    val icon: WeatherIconType,
    val high: String?,
    val low: String?,
)

/** Presentation-layer condition bucket, kept Compose-free so [bose.ankush.weatherify.wear.data.WeatherUiMapper]
 * doesn't need a Compose dependency; screens map this to an actual icon. */
internal enum class WeatherIconType {
    CLEAR,
    CLOUDS,
    RAIN,
    THUNDERSTORM,
    SNOW,

    /** Mist, smoke, haze, dust, fog, sand, ash: the OpenWeather "atmosphere" group. */
    ATMOSPHERE,

    /** Squall, tornado. */
    WIND,
    UNKNOWN,
}
