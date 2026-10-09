package bose.ankush.home.presentation.screen

import bose.ankush.home.domain.model.WeatherCondition
import bose.ankush.home.domain.model.WeatherForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForecastExtrasTest {
    private val zone = forecastZone(IST_OFFSET)

    @Test
    fun rainTodayHiddenWhenNullOrZero() {
        assertNull(formatRainToday(null))
        assertNull(formatRainToday(0.0))
        assertNull(formatRainToday(-1.0))
        assertNull(formatRainToday(Double.NaN))
    }

    @Test
    fun rainTodayFormatsPositiveMillimetres() {
        assertEquals("4.2 mm", formatRainToday(4.2))
        assertEquals("1 mm", formatRainToday(1.0))
        assertEquals("0.5 mm", formatRainToday(0.5))
    }

    @Test
    fun nextRainReturnsNullWhenNoPrecipAhead() {
        val hourly =
            listOf(
                hour(NOW + 3600, clear()),
                hour(NOW + 7200, clear()),
            )
        assertNull(findNextRainLabel(hourly, NOW, zone))
    }

    @Test
    fun nextRainFindsFirstRainSlotInForecastZone() {
        val hourly =
            listOf(
                hour(NOW + 3600, clear()),
                hour(RAIN_AT_16, rain()),
                hour(NOW + 8 * 3600, rain()),
            )
        assertEquals("4 PM", findNextRainLabel(hourly, NOW, zone))
    }

    @Test
    fun nextRainAcceptsThunderAndDrizzleIds() {
        assertEquals("4 PM", findNextRainLabel(listOf(hour(RAIN_AT_16, thunder())), NOW, zone))
        assertEquals("4 PM", findNextRainLabel(listOf(hour(RAIN_AT_16, drizzle())), NOW, zone))
    }

    @Test
    fun nextRainShowsNowForCurrentHour() {
        val hourly = listOf(hour(NOW, rain()), hour(NOW + 3600, clear()))
        assertEquals("Now", findNextRainLabel(hourly, NOW, zone))
    }

    @Test
    fun nextRainIgnoresPastHours() {
        val hourly =
            listOf(
                hour(NOW - 3600, rain()),
                hour(NOW + 2 * 3600, rain()),
            )
        assertEquals("2 PM", findNextRainLabel(hourly, NOW, zone))
    }

    @Test
    fun temperatureTrendCooling() {
        val trend =
            findTemperatureTrend(
                currentTempKelvin = K_30,
                hourly = listOf(hour(NOW + 3 * 3600, clear(), temp = K_27)),
                nowEpochSeconds = NOW,
                zone = zone,
            )
        assertEquals(TemperatureTrendDirection.COOLING, trend?.direction)
        assertEquals("Cooling to 27° by 3 PM", trend?.line)
    }

    @Test
    fun temperatureTrendWarming() {
        val trend =
            findTemperatureTrend(
                currentTempKelvin = K_30,
                hourly = listOf(hour(NOW + 3 * 3600, clear(), temp = K_33)),
                nowEpochSeconds = NOW,
                zone = zone,
            )
        assertEquals(TemperatureTrendDirection.WARMING, trend?.direction)
        assertEquals("Warming to 33° by 3 PM", trend?.line)
    }

    @Test
    fun temperatureTrendSteadyWhenUnderOneDegree() {
        val trend =
            findTemperatureTrend(
                currentTempKelvin = K_30,
                hourly = listOf(hour(NOW + 3 * 3600, clear(), temp = K_30 + 0.4)),
                nowEpochSeconds = NOW,
                zone = zone,
            )
        assertEquals(TemperatureTrendDirection.STEADY, trend?.direction)
        assertEquals("Steady around 30° until 3 PM", trend?.line)
    }

    @Test
    fun temperatureTrendMissingWhenNoFutureHours() {
        assertNull(
            findTemperatureTrend(
                currentTempKelvin = K_30,
                hourly = listOf(hour(NOW - 3600, clear(), temp = K_27)),
                nowEpochSeconds = NOW,
                zone = zone,
            ),
        )
        assertNull(
            findTemperatureTrend(
                currentTempKelvin = null,
                hourly = listOf(hour(NOW + 3 * 3600, clear(), temp = K_27)),
                nowEpochSeconds = NOW,
                zone = zone,
            ),
        )
    }

    @Test
    fun temperatureTrendFallsBackToLastWithinSixHours() {
        // No slot at/after +3h; last within 6h is +2h.
        val trend =
            findTemperatureTrend(
                currentTempKelvin = K_30,
                hourly =
                    listOf(
                        hour(NOW + 3600, clear(), temp = K_29),
                        hour(NOW + 2 * 3600, clear(), temp = K_27),
                    ),
                nowEpochSeconds = NOW,
                zone = zone,
            )
        assertEquals(TemperatureTrendDirection.COOLING, trend?.direction)
        assertEquals("Cooling to 27° by 2 PM", trend?.line)
    }

    @Test
    fun temperatureTrendPicksNearestAtOrAfterThreeHours() {
        val trend =
            findTemperatureTrend(
                currentTempKelvin = K_30,
                hourly =
                    listOf(
                        hour(NOW + 3 * 3600, clear(), temp = K_28),
                        hour(NOW + 4 * 3600, clear(), temp = K_27),
                    ),
                nowEpochSeconds = NOW,
                zone = zone,
            )
        assertEquals("Cooling to 28° by 3 PM", trend?.line)
    }

    @Test
    fun forecastExtrasUsesForecastZoneAndOmitsEmpty() {
        val forecast =
            WeatherForecast(
                id = 1,
                current =
                    WeatherForecast.Current(
                        clouds = 10,
                        dt = NOW,
                        feels_like = K_30,
                        humidity = 40,
                        pressure = 1010,
                        sunrise = null,
                        sunset = null,
                        temp = K_30,
                        uvi = 5.0,
                        weather = listOf(clear()),
                        wind_gust = null,
                        wind_speed = 1.0,
                    ),
                daily =
                    listOf(
                        WeatherForecast.Daily(
                            clouds = 10,
                            dew_point = null,
                            dt = NOW,
                            humidity = 40,
                            pressure = 1010,
                            rain = 0.0,
                            summary = null,
                            sunrise = null,
                            sunset = null,
                            temp = null,
                            uvi = null,
                            weather = listOf(clear()),
                            wind_gust = null,
                            wind_speed = null,
                        ),
                    ),
                hourly = listOf(hour(NOW + 3 * 3600, clear(), temp = K_30)),
                timezoneOffset = IST_OFFSET,
            )
        val extras = forecast.toForecastExtras(nowEpochSeconds = NOW)
        assertNull(extras.rainToday)
        assertNull(extras.nextRain)
        assertEquals(TemperatureTrendDirection.STEADY, extras.temperatureTrend?.direction)
        assertTrue(extras.temperatureTrend?.line?.contains("until 3 PM") == true)
    }

    @Test
    fun hourClockOmitsMinutes() {
        assertEquals("12 AM", toHourClock(MIDNIGHT_IST, zone))
        assertEquals("12 PM", toHourClock(NOON_IST, zone))
        assertEquals("4 PM", toHourClock(RAIN_AT_16, zone))
    }

    private fun hour(
        dt: Long,
        condition: WeatherCondition,
        temp: Double = K_30,
    ): WeatherForecast.Hourly =
        WeatherForecast.Hourly(
            clouds = 10,
            dt = dt,
            feels_like = temp,
            humidity = 40,
            temp = temp,
            weather = listOf(condition),
        )

    private fun clear() = WeatherCondition(description = "clear sky", icon = "01d", id = 800, main = "Clear")

    private fun rain() = WeatherCondition(description = "light rain", icon = "10d", id = 500, main = "Rain")

    private fun thunder() =
        WeatherCondition(description = "thunderstorm", icon = "11d", id = 200, main = "Thunderstorm")

    private fun drizzle() =
        WeatherCondition(description = "light intensity drizzle", icon = "09d", id = 300, main = "Drizzle")

    private companion object {
        const val IST_OFFSET = 19800

        // 2026-10-05 12:00 IST = 2026-10-05 06:30 UTC
        const val NOW = 1791181800L

        // 2026-10-05 16:00 IST
        const val RAIN_AT_16 = NOW + 4 * 3600

        // 2026-10-05 00:00 IST and 12:00 IST
        const val MIDNIGHT_IST = NOW - 12 * 3600
        const val NOON_IST = NOW

        const val K_27 = 300.15
        const val K_28 = 301.15
        const val K_29 = 302.15
        const val K_30 = 303.15
        const val K_33 = 306.15
    }
}
