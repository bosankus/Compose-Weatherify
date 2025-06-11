package bose.ankush.weatherify.presentation

import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.domain.model.WeatherForecast
import bose.ankush.weatherify.domain.model.AirQuality

data class UIState(
    val isLoading: Boolean = false,
    val userLocation: Pair<Double, Double>? = null,
    val weatherData: WeatherForecast? = null,
    val airQualityData: AirQuality? = null,
    val error: UiText? = null
)
