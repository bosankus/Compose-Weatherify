package bose.ankush.weatherify.presentation

import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.WeatherForecast

/**
 * Data class representing the UI state for the weather application.
 * This class encapsulates all the information needed to render the UI.
 *
 * @property isLoading Indicates whether data is currently being loaded.
 * @property userLocation The geographical coordinates (latitude, longitude) of the user's location.
 * @property weatherData The weather forecast data for the user's location.
 * @property airQualityData The air quality information for the user's location.
 * @property error Any error message that should be displayed to the user.
 */
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
)
