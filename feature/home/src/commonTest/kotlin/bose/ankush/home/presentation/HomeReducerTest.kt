package bose.ankush.home.presentation

import bose.ankush.home.domain.location.LocationProblem
import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.ForecastPlace
import bose.ankush.home.domain.model.HomeWeather
import bose.ankush.home.domain.model.HomeWeather.Status
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.domain.model.WeatherSnapshot
import bose.ankush.home.domain.nearby.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeReducerTest {
    @Test
    fun error_clearsLoadingAndReplacesOfflineNotice() {
        val initial = HomeState(isLoading = true, notice = HomeState.Notice.Offline("old offline"))

        val result = HomeReducer.reduce(initial, HomeAction.Error(message = "Boom"))

        assertFalse(result.isLoading)
        assertEquals("Boom", result.error)
        assertFalse(result.isOffline)
        assertNull(result.offlineMessage)
    }

    @Test
    fun pendingWithoutForecast_isLoading() {
        val result = reduce(HomeWeather(status = Status.Pending))

        assertTrue(result.isLoading)
    }

    @Test
    fun fetchingOverAReadyForecast_isLoading() {
        val result =
            reduce(HomeWeather(snapshot = SNAPSHOT, status = Status.Ready, isFetching = true))

        assertTrue(result.isLoading)
        assertEquals(SNAPSHOT.forecast, result.weatherData)
    }

    @Test
    fun ready_populatesForecastAirQualityAndPlace() {
        val initial = HomeState(notice = HomeState.Notice.Offline("old offline"))

        val result = reduce(
            HomeWeather(
                snapshot = SNAPSHOT,
                place = PINNED,
                cacheChecked = true,
                status = Status.Ready
            ), initial
        )

        assertFalse(result.isLoading)
        assertNull(result.error)
        assertFalse(result.isOffline)
        assertNull(result.offlineMessage)
        assertTrue(result.hasCheckedCache)
        assertEquals(SNAPSHOT.forecast, result.weatherData)
        assertEquals(SNAPSHOT.airQuality, result.airQualityData)
        assertEquals(PINNED, result.place)
        assertEquals("Bengaluru", result.pinnedPlaceName)
    }

    @Test
    fun failedWithForecast_isOfflineNotError() {
        val result = reduce(
            HomeWeather(snapshot = SNAPSHOT, status = Status.Failed(Exception())),
            message = "No network"
        )

        assertTrue(result.isOffline)
        assertEquals("No network", result.offlineMessage)
        assertNull(result.error)
    }

    @Test
    fun failedWithoutForecast_isError() {
        val result =
            reduce(HomeWeather(status = Status.Failed(Exception())), message = "No network")

        assertFalse(result.isLoading)
        assertEquals("No network", result.error)
        assertFalse(result.isOffline)
    }

    @Test
    fun noLocationWithKnownCause_isOfflineWithItsFlag() {
        val result =
            reduce(
                HomeWeather(
                    status = Status.NoLocation,
                    locationProblem = LocationProblem.PermissionDenied
                ),
                message = "Permission denied",
            )

        assertTrue(result.isOffline)
        assertEquals("Permission denied", result.offlineMessage)
        assertNull(result.error)
        assertTrue(result.isLocationPermissionDenied)
        assertFalse(result.isGpsDisabled)
    }

    @Test
    fun noLocationWithoutCause_isError() {
        val result = reduce(HomeWeather(status = Status.NoLocation), message = "No coordinates")

        assertEquals("No coordinates", result.error)
        assertFalse(result.isOffline)
    }

    @Test
    fun unpinnedPlace_hasNoPinnedName() {
        val result =
            reduce(HomeWeather(place = PINNED.copy(isPinned = false), status = Status.Ready))

        assertEquals("Bengaluru", result.place?.name)
        assertNull(result.pinnedPlaceName)
    }

    @Test
    fun updateNotificationBanner_showsWhenNotDismissed() {
        val result =
            HomeReducer.reduce(
                HomeState(notificationBanner = HomeState.NotificationBanner(isDismissed = false)),
                HomeAction.UpdateNotificationBanner(show = true, isPermanentlyDeclined = false),
            )

        assertTrue(result.notificationBanner.isVisible)
        assertFalse(result.notificationBanner.isPermanentlyDeclined)
        assertFalse(result.notificationBanner.isDismissed)
    }

    @Test
    fun updateNotificationBanner_respectsPriorDismissalUnlessReset() {
        val dismissed =
            HomeState(
                notificationBanner = HomeState.NotificationBanner(
                    isVisible = false,
                    isDismissed = true
                )
            )

        val withoutReset =
            HomeReducer.reduce(
                dismissed,
                HomeAction.UpdateNotificationBanner(show = true, resetDismissal = false),
            )
        assertFalse(withoutReset.notificationBanner.isVisible)
        assertTrue(withoutReset.notificationBanner.isDismissed)

        val withReset =
            HomeReducer.reduce(
                dismissed,
                HomeAction.UpdateNotificationBanner(show = true, resetDismissal = true),
            )
        assertTrue(withReset.notificationBanner.isVisible)
        assertFalse(withReset.notificationBanner.isDismissed)
    }

    @Test
    fun updateNotificationBanner_preservesPermanentlyDeclinedWhenNull() {
        val initial =
            HomeState(notificationBanner = HomeState.NotificationBanner(isPermanentlyDeclined = true))

        val result =
            HomeReducer.reduce(
                initial,
                HomeAction.UpdateNotificationBanner(show = false, isPermanentlyDeclined = null),
            )

        assertTrue(result.notificationBanner.isPermanentlyDeclined)
    }

    @Test
    fun dismissNotificationBanner_hidesAndMarksDismissed() {
        val initial = HomeState(
            notificationBanner = HomeState.NotificationBanner(
                isVisible = true,
                isDismissed = false
            )
        )

        val result = HomeReducer.reduce(initial, HomeAction.DismissNotificationBanner)

        assertFalse(result.notificationBanner.isVisible)
        assertTrue(result.notificationBanner.isDismissed)
    }

    @Test
    fun resetKeepsChipUpUntilGpsWeatherLands() {
        val started = HomeReducer.reduce(
            HomeState(place = PINNED),
            HomeAction.ResetLocationStarted("Bengaluru")
        )
        assertTrue(started.isResettingLocation)

        // The new attempt has started and already resolved GPS, so the place is no longer pinned.
        val gps = PINNED.copy(isPinned = false)
        val pending =
            reduce(HomeWeather(snapshot = SNAPSHOT, place = gps, status = Status.Pending), started)
        assertNull(pending.pinnedPlaceName)
        assertEquals("Bengaluru", pending.locationChipName)
        assertTrue(pending.isResettingLocation)

        val fetching = reduce(
            HomeWeather(
                snapshot = SNAPSHOT,
                place = gps,
                status = Status.Ready,
                isFetching = true
            ), pending
        )
        assertTrue(fetching.isResettingLocation)

        val landed =
            reduce(HomeWeather(snapshot = SNAPSHOT, place = gps, status = Status.Ready), fetching)
        assertFalse(landed.isResettingLocation)
        assertNull(landed.locationChipName)
    }

    @Test
    fun failedResetStopsSpinner() {
        val started = HomeReducer.reduce(
            HomeState(place = PINNED),
            HomeAction.ResetLocationStarted("Bengaluru")
        )

        assertFalse(
            reduce(
                HomeWeather(snapshot = SNAPSHOT, status = Status.Failed(Exception())),
                started
            ).isResettingLocation
        )
        assertFalse(HomeReducer.reduce(started, HomeAction.ResetLocationFailed).isResettingLocation)
    }

    private fun reduce(
        weather: HomeWeather,
        state: HomeState = HomeState(),
        message: String? = null,
    ): HomeState = HomeReducer.reduce(state, HomeAction.WeatherChanged(weather, message))

    private companion object {
        val SNAPSHOT = WeatherSnapshot(WeatherForecast(id = 42L), AirQuality(id = 7L, aqi = 3))
        val PINNED =
            ForecastPlace(name = "Bengaluru", isPinned = true, point = GeoPoint(12.34, 56.78))
    }
}
