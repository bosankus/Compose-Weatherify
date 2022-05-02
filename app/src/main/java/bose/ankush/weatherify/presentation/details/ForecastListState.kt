package bose.ankush.weatherify.presentation.details

import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.domain.model.AvgForecast

data class ForecastListState(
    val isLoading: Boolean = false,
    val forecasts: List<AvgForecast> = emptyList(),
    val error: UiText? = null
)
