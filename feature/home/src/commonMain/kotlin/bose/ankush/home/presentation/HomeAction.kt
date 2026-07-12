package bose.ankush.home.presentation

import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast

internal sealed interface HomeAction {
    data class Loading(
        val isRefreshing: Boolean = false,
    ) : HomeAction

    data class Error(
        val message: String?,
        val isGpsDisabled: Boolean = false,
    ) : HomeAction

    data class SetOffline(
        val message: String?,
        val isOffline: Boolean = false,
        val isGpsDisabled: Boolean = false,
        val isLocationPermissionDenied: Boolean = false,
    ) : HomeAction

    data class Success(
        val weather: WeatherForecast? = null,
        val airQuality: AirQuality? = null,
        val location: Pair<Double, Double>? = null,
        val isLocationOverridden: Boolean = false,
        val overrideLocationName: String? = null,
    ) : HomeAction

    data class UpdateNotificationBanner(
        val show: Boolean = false,
        val isPermanentlyDeclined: Boolean? = null,
        val resetDismissal: Boolean = false,
    ) : HomeAction

    data object DismissNotificationBanner : HomeAction
}
