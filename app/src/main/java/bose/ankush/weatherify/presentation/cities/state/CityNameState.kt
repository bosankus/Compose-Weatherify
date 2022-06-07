package bose.ankush.weatherify.presentation.cities.state

import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.domain.model.CityName

data class CityNameState(
    val isLoading: Boolean = false,
    val names: List<CityName> = emptyList(),
    val error: UiText? = null
)
