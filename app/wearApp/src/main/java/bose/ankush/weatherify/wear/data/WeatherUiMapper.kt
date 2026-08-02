package bose.ankush.weatherify.wear.data

import bose.ankush.network.model.WeatherForecast
import bose.ankush.weatherify.wear.presentation.AlertUiState
import bose.ankush.weatherify.wear.presentation.DayPhase
import bose.ankush.weatherify.wear.presentation.HourlyUiState
import bose.ankush.weatherify.wear.presentation.WeatherIconType
import bose.ankush.weatherify.wear.presentation.WeatherUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant

// The forecast is synced from the phone with raw OpenWeather values, which are in Kelvin;
// mirrors toCelsius() in :feature:home (internal there, so not importable).
private const val KELVIN_OFFSET = 273.15

private fun Double.toCelsius(): Int = (this - KELVIN_OFFSET).roundToInt()

internal object WeatherUiMapper {
    fun mapToUiState(
        forecast: WeatherForecast,
        locationName: String,
    ): WeatherUiState {
        val current = forecast.data?.current
        val todayTemp = forecast.data?.daily?.firstOrNull()?.temp
        return WeatherUiState(
            location = locationName,
            condition =
                current
                    ?.weather
                    ?.firstOrNull()
                    ?.description
                    ?.replaceFirstChar { it.uppercase() },
            icon = iconTypeFor(current?.weather?.firstOrNull()?.main),
            dayPhase = dayPhaseFor(current),
            temperature = current?.temp?.toCelsius()?.let { "$it°C" } ?: "--",
            feelsLike = current?.feelsLike?.toCelsius()?.let { "$it°" },
            highTemp = todayTemp?.max?.toCelsius()?.let { "$it°" },
            lowTemp = todayTemp?.min?.toCelsius()?.let { "$it°" },
            humidity = current?.humidity?.let { "$it%" },
            wind = current?.windSpeed?.roundToInt()?.let { "$it m/s" },
            hourly = forecast.data?.hourly.orEmpty().mapNotNull { it?.let(::mapHourly) }.take(8),
            alert = forecast.data?.alerts?.firstOrNull()?.let(::mapAlert),
        )
    }

    // Sunrise/sunset come from the synced forecast, so the phase reflects actual daylight
    // at the weather's location, not a fixed clock schedule.
    private const val TWILIGHT_WINDOW_SECONDS = 45 * 60L

    private fun dayPhaseFor(current: WeatherForecast.Data.Current?): DayPhase {
        val dt = current?.dt
        val sunrise = current?.sunrise
        val sunset = current?.sunset
        if (dt == null || sunrise == null || sunset == null) {
            // No sun data in the sync — fall back to the watch's local clock.
            val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
            return when (hour) {
                in 7..17 -> DayPhase.DAY
                6, 18 -> DayPhase.TWILIGHT
                else -> DayPhase.NIGHT
            }
        }
        return when {
            abs(dt - sunrise) <= TWILIGHT_WINDOW_SECONDS ||
                    abs(dt - sunset) <= TWILIGHT_WINDOW_SECONDS -> DayPhase.TWILIGHT

            dt in sunrise..sunset -> DayPhase.DAY
            else -> DayPhase.NIGHT
        }
    }

    private fun mapAlert(alert: WeatherForecast.Data.Alert): AlertUiState? {
        val event = alert.event ?: return null
        return AlertUiState(
            event = event,
            description = alert.description ?: "",
            sender = alert.senderName,
            start = alert.start?.let(::formatFullDateTime),
            end = alert.end?.let(::formatFullDateTime),
        )
    }

    private fun mapHourly(item: WeatherForecast.Data.Hourly): HourlyUiState? {
        val dt = item.dt ?: return null
        val temp = item.temp?.toCelsius() ?: return null
        return HourlyUiState(
            time = formatHour(dt),
            temperature = "$temp°",
            icon = iconTypeFor(item.weather?.firstOrNull()?.main),
        )
    }

    private fun formatHour(epochSeconds: Long): String {
        val now =
            Instant
                .fromEpochSeconds(epochSeconds)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.hour}:${now.minute.toString().padStart(2, '0')}"
    }

    private fun formatFullDateTime(epochSeconds: Long): String {
        val dt =
            Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.date.day} ${dt.month.name.take(3)}, ${dt.hour}:${
            dt.minute.toString().padStart(2, '0')
        }"
    }

    private fun iconTypeFor(main: String?): WeatherIconType =
        when (main?.lowercase()) {
            "clear" -> WeatherIconType.CLEAR
            "clouds" -> WeatherIconType.CLOUDS
            "rain", "drizzle" -> WeatherIconType.RAIN
            "thunderstorm" -> WeatherIconType.THUNDERSTORM
            "snow" -> WeatherIconType.SNOW
            "mist", "smoke", "haze", "dust", "fog", "sand", "ash" -> WeatherIconType.ATMOSPHERE
            "squall", "tornado" -> WeatherIconType.WIND
            else -> WeatherIconType.UNKNOWN
        }
}
