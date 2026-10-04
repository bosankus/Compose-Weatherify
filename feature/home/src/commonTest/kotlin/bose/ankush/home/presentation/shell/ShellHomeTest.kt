package bose.ankush.home.presentation.shell

import bose.ankush.home.domain.model.WeatherCondition
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.network.model.PlaceEvent
import bose.ankush.network.model.SavedLocation
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShellHomeTest {
    @Test
    fun forecast_usesLiveFieldsAndLeavesWindDirectionOut() {
        val forecast =
            WeatherForecast(
                id = 1,
                current =
                    WeatherForecast.Current(
                        clouds = null,
                        dt = null,
                        feels_like = 290.15,
                        humidity = null,
                        pressure = null,
                        sunrise = null,
                        sunset = null,
                        temp = 289.15,
                        uvi = 3.0,
                        weather = listOf(WeatherCondition(description = "fog", id = 741, main = "Fog")),
                        wind_gust = null,
                        wind_speed = 2.5,
                    ),
            )

        val shell = forecast.toShellForecast(place = "Harbor", today = LocalDate(2026, 10, 4))

        assertEquals("16°, Harbor", shell.temperatureLine)
        assertEquals("It's fog", shell.conditionLine)
        assertEquals("17°", shell.feel)
        assertEquals("2.5 m/s", shell.wind)
        assertEquals("3.0", shell.uv)
        assertFalse(shell.wind.contains("WSW"))
    }

    @Test
    fun forecast_missingUvIsADash() {
        val forecast =
            WeatherForecast(
                id = 1,
                current =
                    WeatherForecast.Current(
                        clouds = null,
                        dt = null,
                        feels_like = null,
                        humidity = null,
                        pressure = null,
                        sunrise = null,
                        sunset = null,
                        temp = null,
                        uvi = null,
                        weather = null,
                        wind_gust = null,
                        wind_speed = null,
                    ),
            )

        val shell = forecast.toShellForecast(place = null, today = LocalDate(2026, 10, 4))

        assertEquals(MISSING, shell.temperatureLine)
        assertEquals(MISSING, shell.uv)
        assertEquals(MISSING, shell.wind)
        assertTrue(shell.days.isEmpty())
    }

    @Test
    fun savedPlace_matchesWithinOneKilometreOnly() {
        val near = SavedLocation(id = "a", name = "Near", lat = 10.0, lon = 10.0)
        val far = SavedLocation(id = "b", name = "Far", lat = 10.02, lon = 10.0)

        assertEquals("Near", closestSavedPlace(listOf(far, near), lat = 10.001, lon = 10.0)?.name)
        assertNull(closestSavedPlace(listOf(far), lat = 10.0, lon = 10.0))
    }

    @Test
    fun emptyEvents_markNoDays() {
        assertTrue(eventDatesFrom(emptyList()).isEmpty())
    }

    @Test
    fun events_markTheLocalDateAndSkipBlankStartsAt() {
        val startsAt = "2026-10-04T18:30:00Z"
        val dates =
            eventDatesFrom(
                listOf(
                    PlaceEvent(startsAt = startsAt, title = "Dinner"),
                    PlaceEvent(startsAt = "", title = "Broken"),
                ),
            )
        assertEquals(setOf(eventLocalDate(startsAt)), dates)
        assertNotNull(eventLocalDate(startsAt))
    }

    @Test
    fun create_requiresTitleTimeAndPlace() {
        assertFalse(
            canSubmitEvent(
                ShellState(
                    title = " ",
                    dateText = "2026-10-04",
                    timeText = "09:30",
                    placeName = "Harbor",
                    lat = 1.0,
                    lon = 2.0,
                ),
            ),
        )
        assertFalse(
            canSubmitEvent(ShellState(title = "Dinner", placeName = "Harbor", lat = 1.0, lon = 2.0)),
        )
        assertFalse(
            canSubmitEvent(
                ShellState(title = "Dinner", dateText = "2026-10-04", timeText = "09:30", lat = 1.0, lon = 2.0),
            ),
        )
        assertNotNull(startsAtOrNull("2026-10-04", "09:30"))
        assertTrue(
            canSubmitEvent(
                ShellState(
                    title = "Dinner",
                    dateText = "2026-10-04",
                    timeText = "09:30",
                    placeName = "Harbor",
                    lat = 1.0,
                    lon = 2.0,
                ),
            ),
        )
    }

    @Test
    fun settingsTab_opensSettingsWithoutLeavingHomeSelected() {
        val reduced = ShellReducer.reduce(ShellState(), ShellIntent.SelectTab(ShellTab.SETTINGS))

        assertEquals(ShellTab.HOME, reduced.state.tab)
        assertEquals(ShellEffect.OpenSettings, reduced.effect)
    }

    @Test
    fun savedTab_switchesContent() {
        val reduced = ShellReducer.reduce(ShellState(), ShellIntent.SelectTab(ShellTab.SAVED))

        assertEquals(ShellTab.SAVED, reduced.state.tab)
        assertNull(reduced.effect)
    }
}
