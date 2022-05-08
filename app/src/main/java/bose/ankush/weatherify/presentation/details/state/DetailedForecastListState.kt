package bose.ankush.weatherify.presentation.details.state

import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.data.remote.dto.ForecastDto

data class DetailedForecastListState(
    val forecastList: List<ForecastDto.ForecastList> = emptyList(),
    val error: UiText? = null
)
