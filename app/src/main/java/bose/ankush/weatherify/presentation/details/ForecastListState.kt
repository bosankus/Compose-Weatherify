package bose.ankush.weatherify.presentation.details

import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.data.remote.dto.ForecastDto

data class ForecastListState(
    val isLoading: Boolean = false,
    val forecasts: List<ForecastDto.ForecastList> = emptyList(),
    val error: UiText? = null
)
