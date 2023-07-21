package bose.ankush.weatherify.presentation

import bose.ankush.weatherify.base.common.UiText

data class UIState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val error: UiText? = null
)