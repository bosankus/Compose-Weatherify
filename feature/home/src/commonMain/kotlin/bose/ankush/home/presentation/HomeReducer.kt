package bose.ankush.home.presentation

import bose.ankush.home.domain.model.HomeWeather
import bose.ankush.home.domain.model.HomeWeather.Status
import bose.ankush.home.presentation.HomeState.Notice

internal object HomeReducer {
    fun reduce(
        state: HomeState,
        action: HomeAction,
    ): HomeState =
        when (action) {
            is HomeAction.WeatherChanged -> reduceWeather(state, action.weather, action.message)

            is HomeAction.Error -> state.copy(
                isLoading = false,
                notice = Notice.Error(action.message)
            )

            is HomeAction.UpdateNotificationBanner -> {
                val banner = state.notificationBanner
                val isDismissed = !action.resetDismissal && banner.isDismissed
                state.copy(
                    notificationBanner =
                        HomeState.NotificationBanner(
                            isVisible = action.show && !isDismissed,
                            isPermanentlyDeclined = action.isPermanentlyDeclined
                                ?: banner.isPermanentlyDeclined,
                            isDismissed = isDismissed,
                        ),
                )
            }

            is HomeAction.ResetLocationStarted -> state.copy(resettingFrom = action.name)

            HomeAction.ResetLocationFailed -> state.copy(resettingFrom = null)

            is HomeAction.DismissNotificationBanner ->
                state.copy(
                    notificationBanner = state.notificationBanner.copy(
                        isVisible = false,
                        isDismissed = true
                    ),
                )
        }

    /**
     * A failure with a forecast on screen is an offline notice; without one it is the error page.
     * Missing location reads the same way: offline with a known cause, an error without.
     */
    private fun reduceWeather(
        state: HomeState,
        weather: HomeWeather,
        message: String?,
    ): HomeState {
        val notice =
            when (weather.status) {
                is Status.Failed -> if (weather.snapshot != null) Notice.Offline(message) else Notice.Error(
                    message
                )

                Status.NoLocation ->
                    if (weather.locationProblem != null) Notice.Offline(message) else Notice.Error(
                        message
                    )

                Status.Pending, Status.Ready -> null
            }
        return state.copy(
            isLoading = weather.status is Status.Pending || weather.isFetching,
            hasCheckedCache = weather.cacheChecked,
            place = weather.place,
            snapshot = weather.snapshot,
            notice = notice,
            locationProblem = weather.locationProblem,
            // A reset ends once its attempt has settled, whether on GPS weather or a failure.
            resettingFrom =
                state.resettingFrom?.takeIf { weather.status is Status.Pending || weather.isFetching },
        )
    }
}
