package bose.ankush.weatherify.presentation

import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.WeatherForecast

data class UIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val userLocation: Pair<Double, Double>? = null,
    val weatherData: WeatherForecast? = null,
    val airQualityData: AirQuality? = null,
    val error: UiText? = null,
    val isGpsDisabled: Boolean = false,
    val isLocationOverridden: Boolean = false,
    val activeLocationName: String? = null,
    val isOffline: Boolean = false,
)
