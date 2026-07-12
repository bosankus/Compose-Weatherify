package bose.ankush.home.presentation

import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast

internal data class HomeState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val userLocation: Pair<Double, Double>? = null,
    val weatherData: WeatherForecast? = null,
    val airQualityData: AirQuality? = null,
    val error: String? = null,
    val isGpsDisabled: Boolean = false,
    val isLocationPermissionDenied: Boolean = false,
    val isLocationOverridden: Boolean = false,
    val activeLocationName: String? = null,
    val isOffline: Boolean = false,
    val offlineMessage: String? = null,
    val showNotificationBanner: Boolean = false,
    val isNotificationPermissionPermanentlyDeclined: Boolean = false,
    val isNotificationBannerDismissed: Boolean = false,
)
