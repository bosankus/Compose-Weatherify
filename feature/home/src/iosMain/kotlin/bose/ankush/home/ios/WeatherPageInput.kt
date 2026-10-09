package bose.ankush.home.ios

import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.presentation.HomeState
import bose.ankush.home.presentation.screen.AirLevel
import bose.ankush.home.presentation.screen.CalendarStripDay
import bose.ankush.home.presentation.screen.ForecastExtras
import bose.ankush.home.presentation.screen.HomeWeatherContent
import bose.ankush.home.presentation.screen.airLevelFor
import bose.ankush.home.presentation.screen.displaySections
import bose.ankush.home.presentation.screen.dominant
import bose.ankush.home.presentation.screen.forecastZone
import bose.ankush.home.presentation.screen.formatAnimatedNumber
import bose.ankush.home.presentation.screen.formatAqi
import bose.ankush.home.presentation.screen.formatConcentration
import bose.ankush.home.presentation.screen.parseAlertDescription
import bose.ankush.home.presentation.screen.pollutantReadings
import bose.ankush.home.presentation.screen.sunAlongDay
import bose.ankush.home.presentation.screen.toAlertRows
import bose.ankush.home.presentation.screen.toClock
import bose.ankush.home.presentation.screen.toIssuedLabel
import bose.ankush.home.presentation.util.AirQualityIndexAnalyser
import bose.ankush.home.presentation.util.formatTextCapitalization
import bose.ankush.home.presentation.util.getIconUrl
import bose.ankush.home.presentation.util.toCelsius
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/** Inputs for the weather page, gathered by the controller. */
internal class WeatherPageInput(
    val state: HomeState,
    val content: HomeWeatherContent,
    val extras: ForecastExtras,
    val eventDates: Set<LocalDate>,
    val forecastFailed: Boolean,
    val nearby: NearbyUi?,
)

internal fun WeatherPageInput.toWeatherPageUi(): WeatherPageUi {
    val forecast = state.weatherData
    val current = forecast?.current
    return WeatherPageUi(
        details = detailCells(content, current, extras),
        report = current?.toReport(forecast.timezoneOffset),
        trend = extras.temperatureTrend?.let { TrendUi(line = it.line, direction = it.direction) },
        week = weekUi(content.days, eventDates, forecastFailed),
        alerts = forecast?.alerts.orEmpty().toAlertUis(),
        airQuality = state.airQualityData?.takeIf { it.aqi > 0 }?.toAirQualityUi(),
        hourly = forecast?.hourly.orEmpty().toHourUis(),
        showPromoCards = content.showSmallCards,
        nearby = nearby,
    )
}

/** Feel, wind, UV, humidity, pressure and clouds; rain cells only when they have a value. */
private fun detailCells(
    content: HomeWeatherContent,
    current: WeatherForecast.Current?,
    extras: ForecastExtras,
): List<DetailCellUi> =
    buildList {
        add(DetailCellUi(DetailKind.FEEL, "Feel", content.feel, null))
        val gust =
            current?.wind_gust?.let {
                "Gust ${
                    formatAnimatedNumber(
                        it,
                        decimals = if (it % 1.0 == 0.0) 0 else 1,
                        suffix = " m/s"
                    )
                }"
            }
        add(DetailCellUi(DetailKind.WIND, "Wind", content.wind, gust))
        add(DetailCellUi(DetailKind.UV, "UV", content.uv, null))
        val humidity = formatAnimatedNumber(current?.humidity?.toDouble(), suffix = "%")
        add(DetailCellUi(DetailKind.HUMIDITY, "Humidity", humidity, null))
        val pressure = formatAnimatedNumber(current?.pressure?.toDouble(), suffix = " hPa")
        add(DetailCellUi(DetailKind.PRESSURE, "Pressure", pressure, null))
        val clouds = formatAnimatedNumber(current?.clouds?.toDouble(), suffix = "%")
        add(DetailCellUi(DetailKind.CLOUDS, "Clouds", clouds, null))
        extras.rainToday?.let { add(DetailCellUi(DetailKind.RAIN_TODAY, "Rain today", it, null)) }
        extras.nextRain?.let { add(DetailCellUi(DetailKind.NEXT_RAIN, "Next rain", it, null)) }
    }

private fun WeatherForecast.Current.toReport(timezoneOffset: Int?): CurrentReportUi? {
    val weather = weather?.firstOrNull()
    val description = weather?.description?.formatTextCapitalization().orEmpty()
    val zone = forecastZone(timezoneOffset)
    return weather?.takeIf { description.isNotBlank() }?.let {
        CurrentReportUi(
            condition = description,
            glyph = skyGlyph(it.main, it.description, it.icon),
            sunrise = sunrise?.toClock(zone) ?: UNAVAILABLE,
            sunset = sunset?.toClock(zone) ?: UNAVAILABLE,
            sunProgress = sunAlongDay(sunrise, sunset, Clock.System.now().epochSeconds),
        )
    }
}

/** Thunder before rain, snow before rain, so mixed phrases keep the stronger sky. */
private fun skyGlyph(
    main: String?,
    description: String?,
    icon: String?,
): SkyGlyph {
    val text = "$main $description".lowercase()
    val night = icon?.endsWith("n") == true
    return when {
        "thunder" in text -> SkyGlyph.THUNDER
        "snow" in text || "sleet" in text -> SkyGlyph.SNOW
        "drizzle" in text || "rain" in text -> SkyGlyph.RAIN
        "clear" in text -> if (night) SkyGlyph.CLEAR_NIGHT else SkyGlyph.CLEAR_DAY
        MIST_WORDS.any { it in text } -> SkyGlyph.MIST
        else -> SkyGlyph.CLOUDS
    }
}

private fun weekUi(
    days: List<CalendarStripDay>,
    eventDates: Set<LocalDate>,
    forecastFailed: Boolean,
): WeekUi? {
    if (days.isEmpty() && !forecastFailed) return null
    return WeekUi(
        days =
            days.mapIndexed { index, day ->
                DayUi(
                    key = day.day?.toString() ?: "day-$index",
                    label = day.label,
                    date = day.date,
                    isToday = day.selected,
                    hasEvent = day.day != null && day.day in eventDates,
                    iconUrl = day.icon.takeIf { it.isNotBlank() }?.getIconUrl(),
                    range = day.rangeLabel(),
                    caption = day.description,
                )
            },
        showRetry = forecastFailed,
    )
}

private fun CalendarStripDay.rangeLabel(): String =
    when {
        min.isNotEmpty() && max.isNotEmpty() -> "$min / $max"
        min.isNotEmpty() -> min
        max.isNotEmpty() -> max
        else -> temp
    }

private fun List<WeatherForecast.Alert?>.toAlertUis(): List<AlertUi> =
    toAlertRows().mapIndexed { index, row ->
        val alert = row.alert
        val parsed = parseAlertDescription(alert.description)
        val duration = "${alert.start?.toIssuedLabel()} - ${alert.end?.toIssuedLabel()}"
        AlertUi(
            key = "${alert.start}-${alert.event}-$index",
            title = row.title,
            startText = row.startText,
            summary = parsed.summary,
            duration = duration,
            source = alert.sender_name ?: "Source unknown",
            sections =
                parsed.displaySections().mapIndexed { sectionIndex, display ->
                    AlertSectionUi(
                        key = "$sectionIndex-${display.heading}",
                        heading = display.heading,
                        body = display.section.body,
                        items = display.section.items,
                    )
                },
            fallback = parsed.fallback,
        )
    }

private fun AirQuality.toAirQualityUi(): AirQualityUi {
    val readings = pollutantReadings()
    return AirQualityUi(
        status = AirQualityIndexAnalyser.getAQIAnalysedText(aqi).first,
        aqiLabel = "AQI ${formatAqi(aqi)}",
        band = airLevelFor(aqi).toBand(),
        dominant = readings.dominant()?.pollutant?.label,
        readings =
            readings.map { reading ->
                PollutantUi(
                    key = reading.pollutant.name,
                    name = reading.pollutant.label,
                    value = formatConcentration(reading.value),
                    unit = POLLUTANT_UNIT,
                    fraction = reading.fraction,
                    band = reading.level.toBand(),
                )
            },
    )
}

private fun AirLevel.toBand(): AirBand =
    when (this) {
        AirLevel.GOOD -> AirBand.GOOD
        AirLevel.FAIR -> AirBand.FAIR
        AirLevel.MODERATE -> AirBand.MODERATE
        AirLevel.POOR -> AirBand.POOR
        AirLevel.VERY_POOR -> AirBand.VERY_POOR
    }

private fun List<WeatherForecast.Hourly?>.toHourUis(): List<HourUi> =
    mapNotNull { it }
        .filter { it.dt != null }
        .take(HOURLY_LIMIT)
        .map { hour ->
            val weather = hour.weather?.firstOrNull()
            HourUi(
                key = hour.dt.toString(),
                time = hour.dt?.toClock().orEmpty(),
                iconUrl = weather?.icon?.takeIf { it.isNotBlank() }?.getIconUrl(),
                temperature = hour.temp?.let { "${it.toCelsius()}°" }.orEmpty(),
                caption = weather?.description?.formatTextCapitalization().orEmpty(),
            )
        }

private val MIST_WORDS = listOf("mist", "fog", "haze", "smoke", "dust", "sand", "ash")
private const val UNAVAILABLE = "N/A"
private const val POLLUTANT_UNIT = "µg/m³"
private const val HOURLY_LIMIT = 24
