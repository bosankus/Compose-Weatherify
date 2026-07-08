package bose.ankush.home.presentation

internal object HomeReducer {
    fun reduce(
        state: HomeState,
        action: HomeAction,
    ): HomeState =
        when (action) {
            is HomeAction.Loading ->
                state.copy(
                    isLoading = true,
                    isRefreshing = action.isRefreshing,
                    error = null,
                )

            is HomeAction.Error ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = action.message,
                    isGpsDisabled = action.isGpsDisabled,
                )

            is HomeAction.SetOffline ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    isOffline = action.isOffline,
                    offlineMessage = action.message,
                    isGpsDisabled = action.isGpsDisabled,
                )

            is HomeAction.Success ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    isOffline = false,
                    offlineMessage = null,
                    weatherData = action.weather,
                    airQualityData = action.airQuality,
                    userLocation = action.location,
                    isLocationOverridden = action.isLocationOverridden,
                    activeLocationName = action.overrideLocationName,
                )

            is HomeAction.UpdateNotificationBanner -> {
                val isNotificationBannerDismissed =
                    if (action.resetDismissal) false else state.isNotificationBannerDismissed
                state.copy(
                    showNotificationBanner = action.show && !isNotificationBannerDismissed,
                    isNotificationPermissionPermanentlyDeclined =
                        action.isPermanentlyDeclined
                            ?: state.isNotificationPermissionPermanentlyDeclined,
                    isNotificationBannerDismissed = isNotificationBannerDismissed,
                )
            }

            is HomeAction.DismissNotificationBanner ->
                state.copy(showNotificationBanner = false, isNotificationBannerDismissed = true)
        }
}
