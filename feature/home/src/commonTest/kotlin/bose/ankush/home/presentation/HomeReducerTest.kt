package bose.ankush.home.presentation

import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeReducerTest {
    @Test
    fun loading_setsLoadingAndClearsError() {
        val initial =
            HomeState(
                isLoading = false,
                isRefreshing = false,
                error = "previous",
            )

        val result = HomeReducer.reduce(initial, HomeAction.Loading(isRefreshing = false))

        assertTrue(result.isLoading)
        assertFalse(result.isRefreshing)
        assertNull(result.error)
    }

    @Test
    fun loading_withRefreshing_setsRefreshingFlag() {
        val result = HomeReducer.reduce(HomeState(), HomeAction.Loading(isRefreshing = true))

        assertTrue(result.isLoading)
        assertTrue(result.isRefreshing)
        assertNull(result.error)
    }

    @Test
    fun error_clearsLoadingAndSetsMessageAndGpsFlag() {
        val initial = HomeState(isLoading = true, isRefreshing = true)

        val result =
            HomeReducer.reduce(
                initial,
                HomeAction.Error(message = "GPS off", isGpsDisabled = true),
            )

        assertFalse(result.isLoading)
        assertFalse(result.isRefreshing)
        assertEquals("GPS off", result.error)
        assertTrue(result.isGpsDisabled)
    }

    @Test
    fun setOffline_clearsLoadingAndAppliesOfflineFlags() {
        val initial = HomeState(isLoading = true, isRefreshing = true, error = "boom")

        val result =
            HomeReducer.reduce(
                initial,
                HomeAction.SetOffline(
                    message = "Offline cache",
                    isOffline = true,
                    isGpsDisabled = false,
                    isLocationPermissionDenied = true,
                ),
            )

        assertFalse(result.isLoading)
        assertFalse(result.isRefreshing)
        assertNull(result.error)
        assertTrue(result.isOffline)
        assertEquals("Offline cache", result.offlineMessage)
        assertFalse(result.isGpsDisabled)
        assertTrue(result.isLocationPermissionDenied)
    }

    @Test
    fun success_populatesWeatherAirQualityAndLocation() {
        val weather = WeatherForecast(id = 42L)
        val airQuality = AirQuality(id = 7L, aqi = 3)
        val location = 12.34 to 56.78
        val initial =
            HomeState(
                isLoading = true,
                isRefreshing = true,
                error = "old",
                isOffline = true,
                offlineMessage = "old offline",
            )

        val result =
            HomeReducer.reduce(
                initial,
                HomeAction.Success(
                    weather = weather,
                    airQuality = airQuality,
                    location = location,
                    isLocationOverridden = true,
                    overrideLocationName = "Bengaluru",
                ),
            )

        assertFalse(result.isLoading)
        assertFalse(result.isRefreshing)
        assertNull(result.error)
        assertFalse(result.isOffline)
        assertNull(result.offlineMessage)
        assertEquals(weather, result.weatherData)
        assertEquals(airQuality, result.airQualityData)
        assertEquals(location, result.userLocation)
        assertTrue(result.isLocationOverridden)
        assertEquals("Bengaluru", result.activeLocationName)
    }

    @Test
    fun updateNotificationBanner_showsWhenNotDismissed() {
        val result =
            HomeReducer.reduce(
                HomeState(isNotificationBannerDismissed = false),
                HomeAction.UpdateNotificationBanner(show = true, isPermanentlyDeclined = false),
            )

        assertTrue(result.showNotificationBanner)
        assertFalse(result.isNotificationPermissionPermanentlyDeclined)
        assertFalse(result.isNotificationBannerDismissed)
    }

    @Test
    fun updateNotificationBanner_respectsPriorDismissalUnlessReset() {
        val dismissed =
            HomeState(
                showNotificationBanner = false,
                isNotificationBannerDismissed = true,
            )

        val withoutReset =
            HomeReducer.reduce(
                dismissed,
                HomeAction.UpdateNotificationBanner(show = true, resetDismissal = false),
            )
        assertFalse(withoutReset.showNotificationBanner)
        assertTrue(withoutReset.isNotificationBannerDismissed)

        val withReset =
            HomeReducer.reduce(
                dismissed,
                HomeAction.UpdateNotificationBanner(show = true, resetDismissal = true),
            )
        assertTrue(withReset.showNotificationBanner)
        assertFalse(withReset.isNotificationBannerDismissed)
    }

    @Test
    fun updateNotificationBanner_preservesPermanentlyDeclinedWhenNull() {
        val initial = HomeState(isNotificationPermissionPermanentlyDeclined = true)

        val result =
            HomeReducer.reduce(
                initial,
                HomeAction.UpdateNotificationBanner(show = false, isPermanentlyDeclined = null),
            )

        assertTrue(result.isNotificationPermissionPermanentlyDeclined)
    }

    @Test
    fun dismissNotificationBanner_hidesAndMarksDismissed() {
        val initial = HomeState(showNotificationBanner = true, isNotificationBannerDismissed = false)

        val result = HomeReducer.reduce(initial, HomeAction.DismissNotificationBanner)

        assertFalse(result.showNotificationBanner)
        assertTrue(result.isNotificationBannerDismissed)
    }

    @Test
    fun updateLeaveByCard_hidesWhenAlreadyDismissed() {
        val dismissed = HomeState(isLeaveByDismissed = true, showLeaveByCard = false)

        val result = HomeReducer.reduce(dismissed, HomeAction.UpdateLeaveByCard(show = true))

        assertFalse(result.showLeaveByCard)
        assertTrue(result.isLeaveByDismissed)
    }

    @Test
    fun dismissLeaveByCard_hidesForTheSession() {
        val result =
            HomeReducer.reduce(
                HomeState(showLeaveByCard = true),
                HomeAction.DismissLeaveByCard,
            )

        assertFalse(result.showLeaveByCard)
        assertTrue(result.isLeaveByDismissed)
    }

    @Test
    fun joinAndMisleading_areSessionFlagsAndSurviveSuccess() {
        val joined =
            HomeReducer.reduce(
                HomeReducer.reduce(
                    HomeState(showLeaveByCard = true),
                    HomeAction.JoinLeaveByList,
                ),
                HomeAction.NoteLeaveByMisleading,
            )
        val result = HomeReducer.reduce(joined, HomeAction.Success(weather = WeatherForecast(id = 1L)))

        assertTrue(result.showLeaveByCard)
        assertTrue(result.hasJoinedLeaveByList)
        assertTrue(result.hasNotedLeaveByMisleading)
    }
}
