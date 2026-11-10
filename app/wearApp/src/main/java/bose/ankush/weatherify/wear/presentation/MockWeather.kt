package bose.ankush.weatherify.wear.presentation

import bose.ankush.network.model.WeatherForecast
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Preview data in the same shape the phone syncs via the Wearable Data Layer
 * (see [bose.ankush.weatherify.wear.data.WeatherSyncStore]). Temperatures are in Kelvin,
 * like the raw OpenWeather payload the mapper expects.
 */
internal const val MOCK_LOCATION_NAME = "London, UK"

private const val K = 273.15

internal val mockWeatherForecast: WeatherForecast by lazy {
    val now = Clock.System.now()
    WeatherForecast(
        status = true,
        message = null,
        data =
            WeatherForecast.Data(
                current =
                    WeatherForecast.Data.Current(
                        dt = now.epochSeconds,
                        sunrise = (now - 6.hours).epochSeconds,
                        sunset = (now + 5.hours).epochSeconds,
                        temp = K + 19.0,
                        feelsLike = K + 18.0,
                        humidity = 58,
                        windSpeed = 3.6,
                        uvi = 4.0,
                        pressure = 1013,
                        clouds = 40,
                        weather =
                            listOf(
                                WeatherForecast.Data.WeatherInfo(description = "light rain", main = "Rain"),
                            ),
                    ),
                daily =
                    listOf(
                        "Rain" to (24.0 to 16.0),
                        "Clouds" to (22.0 to 15.0),
                        "Clear" to (25.0 to 14.0),
                        "Clear" to (27.0 to 16.0),
                        "Thunderstorm" to (21.0 to 15.0),
                        "Clouds" to (20.0 to 13.0),
                    ).mapIndexed { i, (main, range) ->
                        WeatherForecast.Data.Daily(
                            dt = (now + i.days).epochSeconds,
                            rain = if (main == "Rain") 2.4 else null,
                            weather =
                                listOf(
                                    WeatherForecast.Data.WeatherInfo(description = main.lowercase(), main = main),
                                ),
                            temp =
                                WeatherForecast.Data.Daily.Temp(
                                    day = null,
                                    eve = null,
                                    max = K + range.first,
                                    min = K + range.second,
                                    morn = null,
                                    night = null,
                                ),
                        )
                    },
                hourly =
                    (1..12).map { i ->
                        WeatherForecast.Data.Hourly(
                            dt = (now + i.hours).epochSeconds,
                            temp = K + 19.0 - i / 2,
                            weather =
                                listOf(
                                    WeatherForecast.Data.WeatherInfo(
                                        description = if (i < 4) "light rain" else "clear sky",
                                        main = if (i < 4) "Rain" else "Clear",
                                    ),
                                ),
                        )
                    },
                alerts =
                    listOf(
                        WeatherForecast.Data.Alert(
                            event = "Flood Warning",
                            senderName = "Met Office",
                            description = "Heavy rain may cause flooding in low-lying areas.",
                        ),
                    ),
            ),
    )
}
