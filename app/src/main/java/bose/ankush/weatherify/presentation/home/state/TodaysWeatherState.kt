package bose.ankush.weatherify.presentation.home.state

import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.domain.model.Weather

data class TodaysWeatherState(
    val isLoading: Boolean = false,
    val weather: Weather? = Weather(),
    val error: UiText? = null
)
