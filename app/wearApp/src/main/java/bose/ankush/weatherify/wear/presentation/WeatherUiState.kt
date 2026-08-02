package bose.ankush.weatherify.wear.presentation

internal data class WeatherUiState(
    val location: String,
    val condition: String?,
    val icon: WeatherIconType,
    val dayPhase: DayPhase,
    val temperature: String,
    val feelsLike: String?,
    val highTemp: String?,
    val lowTemp: String?,
    val humidity: String?,
    val wind: String?,
    val hourly: List<HourlyUiState>,
    val alert: AlertUiState?,
)

/** Coarse time-of-day bucket derived from the synced sunrise/sunset, used to theme the
 * background gradient and text tint: full daylight, the warm band around sunrise/sunset,
 * and low-light night. */
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
    val time: String,
    val temperature: String,
    val icon: WeatherIconType,
)

/** Presentation-layer condition bucket — kept Compose-free so [bose.ankush.weatherify.wear.data.WeatherUiMapper]
 * doesn't need a Compose dependency; [WeatherScreen] maps this to an actual icon.
 * Currently backed by Material icons as placeholders until the real weather SVG icon set lands. */
internal enum class WeatherIconType {
    CLEAR,
    CLOUDS,
    RAIN,
    THUNDERSTORM,
    SNOW,

    /** Mist, smoke, haze, dust, fog, sand, ash — the OpenWeather "atmosphere" group. */
    ATMOSPHERE,

    /** Squall, tornado. */
    WIND,
    UNKNOWN,
}
