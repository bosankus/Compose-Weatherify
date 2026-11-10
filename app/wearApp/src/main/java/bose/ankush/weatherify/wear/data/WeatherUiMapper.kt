package bose.ankush.weatherify.wear.data

import bose.ankush.network.model.WeatherForecast
import bose.ankush.weatherify.wear.presentation.AlertUiState
import bose.ankush.weatherify.wear.presentation.DailyUiState
import bose.ankush.weatherify.wear.presentation.DayPhase
import bose.ankush.weatherify.wear.presentation.HourlyUiState
import bose.ankush.weatherify.wear.presentation.TodayUiState
import bose.ankush.weatherify.wear.presentation.WeatherIconType
import bose.ankush.weatherify.wear.presentation.WeatherUiState
import kotlinx.datetime.LocalDateTime
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

private fun Double.toDegrees(): String = "${toCelsius()}°"

// The hourly dial has twelve slots around its ring, like hours on a clock face.
private const val HOURLY_DIAL_SLOTS = 12

internal object WeatherUiMapper {
    fun mapToUiState(
        forecast: WeatherForecast,
        locationName: String,
    ): WeatherUiState {
        val current = forecast.data?.current
        val currentWeather = current?.weather?.firstOrNull()
        val today = forecast.data?.daily?.firstOrNull()
        val uvi = current?.uvi
        return WeatherUiState(
            location = locationName,
            condition = currentWeather?.description?.capitalized(),
            conditionShort = currentWeather?.main?.takeIf { it.isNotBlank() }?.uppercase(),
            icon = iconTypeFor(currentWeather?.main),
            dayPhase = dayPhaseFor(current),
            temperature = current?.temp?.toDegrees() ?: "--",
            feelsLike = current?.feelsLike?.toDegrees(),
            highTemp = today?.temp?.max?.toDegrees(),
            lowTemp = today?.temp?.min?.toDegrees(),
            humidity = current?.humidity?.let { "$it%" },
            wind = current?.windSpeed?.roundToInt()?.toString(),
            uvIndex = uvi?.roundToInt()?.toString(),
            uvLevel = uvi?.let(::uvLevelFor),
            today =
                TodayUiState(
                    sunrise = current?.sunrise?.let(::formatTime),
                    sunset = current?.sunset?.let(::formatTime),
                    airQuality =
                        forecast.data
                            ?.airQuality
                            ?.list
                            ?.firstOrNull()
                            ?.main
                            ?.aqi
                            ?.let(::airQualityFor),
                    pressure = current?.pressure?.let { "$it hPa" },
                    // OpenWeather omits `rain` on dry days, so a present daily entry with no
                    // rain field means zero, not unknown.
                    rain = today?.let { "${((it.rain ?: 0.0) * 10).roundToInt() / 10.0} mm" },
                    clouds = current?.clouds?.let { "$it%" },
                ),
            hourly =
                forecast.data
                    ?.hourly
                    .orEmpty()
                    .mapNotNull { it?.let(::mapHourly) }
                    .take(HOURLY_DIAL_SLOTS),
            daily =
                forecast.data
                    ?.daily
                    .orEmpty()
                    .filterNotNull()
                    .mapIndexedNotNull { index, day -> mapDaily(index, day) },
            alert =
                forecast.data
                    ?.alerts
                    ?.firstOrNull()
                    ?.let(::mapAlert),
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
            // No sun data in the sync, so fall back to the watch's local clock.
            return when (now().hour) {
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
        val temp = item.temp ?: return null
        val weather = item.weather?.firstOrNull()
        val local = dt.toLocal()
        return HourlyUiState(
            time = formatTime(dt),
            hourLabel = local.hour.toString().padStart(2, '0'),
            temperature = temp.toDegrees(),
            condition = weather?.description?.capitalized(),
            icon = iconTypeFor(weather?.main),
        )
    }

    private fun mapDaily(
        index: Int,
        item: WeatherForecast.Data.Daily,
    ): DailyUiState? {
        val dt = item.dt ?: return null
        val weather = item.weather?.firstOrNull()
        return DailyUiState(
            day =
                if (index == 0) {
                    "Today"
                } else {
                    dt
                        .toLocal()
                        .dayOfWeek.name
                        .take(3)
                        .lowercase()
                        .capitalized()
                },
            condition = weather?.description?.capitalized(),
            icon = iconTypeFor(weather?.main),
            high = item.temp?.max?.toDegrees(),
            low = item.temp?.min?.toDegrees(),
        )
    }

    private fun uvLevelFor(uvi: Double): String =
        when {
            uvi < 3 -> "LOW"
            uvi < 6 -> "MOD"
            uvi < 8 -> "HIGH"
            uvi < 11 -> "V.HIGH"
            else -> "EXTREME"
        }

    // OpenWeather's air-pollution index runs 1 (best) to 5 (worst).
    private fun airQualityFor(aqi: Int): String =
        when (aqi) {
            1 -> "Good"
            2 -> "Fair"
            3 -> "Moderate"
            4 -> "Poor"
            5 -> "Very poor"
            else -> "AQI $aqi"
        }

    private fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    private fun Long.toLocal(): LocalDateTime = Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.currentSystemDefault())

    private fun String.capitalized(): String = replaceFirstChar { it.uppercase() }

    private fun formatTime(epochSeconds: Long): String {
        val local = epochSeconds.toLocal()
        return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }

    private fun formatFullDateTime(epochSeconds: Long): String {
        val dt = epochSeconds.toLocal()
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
