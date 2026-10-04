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
        assertEquals("16°", shell.temperature)
        assertEquals("Harbor", shell.place)
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
        assertEquals(MISSING, shell.temperature)
        assertEquals("", shell.place)
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

    @Test
    fun failedOrEmptySections_stayPresentInsteadOfDisappearing() {
        val failedAccount = ShellReducer.reduce(ShellState(), ShellIntent.AccountFailed)
        assertEquals(ShellSectionStatus.Failed, failedAccount.state.accountPhoto)
        assertNull(failedAccount.state.photoUrl)

        val emptyAccount = ShellReducer.reduce(ShellState(), ShellIntent.AccountLoaded(" "))
        assertEquals(ShellSectionStatus.Empty, emptyAccount.state.accountPhoto)
        assertNull(emptyAccount.state.photoUrl)

        val readyAccount = ShellReducer.reduce(ShellState(), ShellIntent.AccountLoaded("https://photo.example"))
        assertEquals(ShellSectionStatus.Ready, readyAccount.state.accountPhoto)
        assertEquals("https://photo.example", readyAccount.state.photoUrl)

        val emptyPlace = ShellReducer.reduce(ShellState(), ShellIntent.SavedPlaceLoaded(null))
        assertEquals(ShellSectionStatus.Empty, emptyPlace.state.savedPlace)
        assertNull(emptyPlace.state.featuredPlace)

        val failedPlace = ShellReducer.reduce(ShellState(), ShellIntent.SavedPlaceFailed)
        assertEquals(ShellSectionStatus.Failed, failedPlace.state.savedPlace)
        assertNull(failedPlace.state.featuredPlace)

        val place = SavedLocation(id = "a", name = "Near", lat = 1.0, lon = 2.0)
        val readyPlace = ShellReducer.reduce(ShellState(), ShellIntent.SavedPlaceLoaded(place))
        assertEquals(ShellSectionStatus.Ready, readyPlace.state.savedPlace)
        assertEquals("Near", readyPlace.state.featuredPlace?.name)

        val emptyEvents = ShellReducer.reduce(ShellState(), ShellIntent.EventsLoaded(emptySet()))
        assertEquals(ShellSectionStatus.Empty, emptyEvents.state.events)
        assertTrue(emptyEvents.state.eventDates.isEmpty())

        val failedEvents = ShellReducer.reduce(ShellState(), ShellIntent.EventsFailed)
        assertEquals(ShellSectionStatus.Failed, failedEvents.state.events)

        val day = LocalDate(2026, 10, 4)
        val readyEvents = ShellReducer.reduce(ShellState(), ShellIntent.EventsLoaded(setOf(day)))
        assertEquals(ShellSectionStatus.Ready, readyEvents.state.events)
        assertEquals(setOf(day), readyEvents.state.eventDates)
    }

    @Test
    fun locationChange_reloadsSavedPlaceAndEventsInsteadOfHidingThem() {
        val start =
            ShellState(
                savedPlace = ShellSectionStatus.Ready,
                featuredPlace = SavedLocation(id = "a", name = "Near", lat = 1.0, lon = 2.0),
                events = ShellSectionStatus.Ready,
                eventDates = setOf(LocalDate(2026, 10, 4)),
            )

        val reduced = ShellReducer.reduce(start, ShellIntent.LocationUpdated(lat = 3.0, lon = 4.0))

        assertEquals(ShellSectionStatus.Loading, reduced.state.savedPlace)
        assertEquals(ShellSectionStatus.Loading, reduced.state.events)
        assertNull(reduced.state.featuredPlace)
        assertTrue(reduced.state.eventDates.isEmpty())
    }

    @Test
    fun forecastSection_keepsLoadingEmptyAndFailedDistinctFromReady() {
        assertEquals(
            ShellSectionStatus.Loading,
            forecastSectionStatus(isLoading = true, hasCurrent = false, hasFailure = false),
        )
        assertEquals(
            ShellSectionStatus.Failed,
            forecastSectionStatus(isLoading = false, hasCurrent = false, hasFailure = true),
        )
        assertEquals(
            ShellSectionStatus.Empty,
            forecastSectionStatus(isLoading = false, hasCurrent = false, hasFailure = false),
        )
        assertEquals(
            ShellSectionStatus.Ready,
            forecastSectionStatus(isLoading = true, hasCurrent = true, hasFailure = true),
        )
    }

    @Test
    fun calendarDays_emptyForecastStaysEmptyInsteadOfHidden() {
        assertEquals(ShellSectionStatus.Empty, calendarDaysStatus(ShellSectionStatus.Ready, dayCount = 0))
        assertEquals(ShellSectionStatus.Ready, calendarDaysStatus(ShellSectionStatus.Ready, dayCount = 7))
        assertEquals(ShellSectionStatus.Failed, calendarDaysStatus(ShellSectionStatus.Failed, dayCount = 0))
        assertEquals(ShellSectionStatus.Loading, calendarDaysStatus(ShellSectionStatus.Loading, dayCount = 0))
    }

    @Test
    fun placeholders_coverLoadingEmptyAndFailedForEverySection() {
        ShellSectionKind.entries.forEach { kind ->
            val loading = placeholderMessage(kind, ShellSectionStatus.Loading)
            val empty = placeholderMessage(kind, ShellSectionStatus.Empty)
            val failed = placeholderMessage(kind, ShellSectionStatus.Failed)
            assertNotNull(loading)
            assertNotNull(empty)
            assertNotNull(failed)
            assertNull(placeholderMessage(kind, ShellSectionStatus.Ready))
            assertEquals(setOf(loading, empty, failed).size, 3)
            assertTrue(retryContentDescription(kind).startsWith("Retry"))
        }
    }
}
