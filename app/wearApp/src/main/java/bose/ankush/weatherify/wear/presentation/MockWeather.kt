package bose.ankush.weatherify.wear.presentation

import bose.ankush.network.model.WeatherForecast
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Fallback shown until a forecast has synced from the phone via the Wearable Data Layer API
 * (see [bose.ankush.weatherify.wear.data.WeatherSyncStore]) — e.g. right after a fresh install,
 * before the paired phone has pushed anything yet.
 */
internal const val mockLocationName = "London, UK"

internal val mockWeatherForecast: WeatherForecast =
    WeatherForecast(
        status = true,
        message = null,
        data =
            WeatherForecast.Data(
                current =
                    WeatherForecast.Data.Current(
                        temp = 19.0,
                        feelsLike = 18.0,
                        humidity = 58,
                        windSpeed = 3.6,
                        uvi = 4.0,
                        pressure = 1013,
                        weather =
                            listOf(
                                WeatherForecast.Data.WeatherInfo(
                                    description = "light rain",
                                    main = "Rain",
                                    icon = "10d",
                                ),
                            ),
                    ),
                daily =
                    listOf(
                        WeatherForecast.Data.Daily(
                            temp =
                                WeatherForecast.Data.Daily.Temp(
                                    day = 22.0,
                                    eve = 18.0,
                                    max = 24.0,
                                    min = 16.0,
                                    morn = 17.0,
                                    night = 16.5,
                                ),
                        ),
                    ),
                hourly = buildMockHourly(),
                alerts =
                    listOf(
                        WeatherForecast.Data.Alert(event = "Flood Warning"),
                    ),
            ),
    )

private fun buildMockHourly(): List<WeatherForecast.Data.Hourly> {
    val now = Clock.System.now()
    val temps = listOf(19.0, 18.0, 18.0, 17.0)
    val mains = listOf("Clouds", "Clouds", "Clear", "Clear")
    return temps.indices.map { i ->
        WeatherForecast.Data.Hourly(
            dt = (now + (i + 1).hours).epochSeconds,
            temp = temps[i],
            weather =
                listOf(
                    WeatherForecast.Data.WeatherInfo(main = mains[i]),
                ),
        )
    }
}
