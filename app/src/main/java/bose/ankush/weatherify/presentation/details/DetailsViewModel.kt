package bose.ankush.weatherify.presentation.details

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.domain.use_case.get_weather_forecasts.GetForecastDetails
import bose.ankush.weatherify.common.ResultData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getForecastDetailsUseCase: GetForecastDetails
) : ViewModel() {

    private val _state = mutableStateOf(ForecastListState())
    val state: State<ForecastListState> = _state

    init {
        getForecastList()
    }

    private fun getForecastList() {
        getForecastDetailsUseCase().onEach { result ->
            when (result) {
                is ResultData.Loading -> _state.value = ForecastListState(isLoading = true)
                is ResultData.Success -> _state.value =
                    ForecastListState(forecasts = result.data ?: emptyList())
                is ResultData.Failed -> _state.value = ForecastListState(error = result.message)
                is ResultData.DoNothing -> {}
            }
        }.launchIn(viewModelScope)
    }
}