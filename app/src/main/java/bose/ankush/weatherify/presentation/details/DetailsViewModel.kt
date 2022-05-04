package bose.ankush.weatherify.presentation.details

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.domain.use_case.get_weather_forecasts.GetForecastDetails
import bose.ankush.weatherify.domain.use_case.get_weather_forecasts.GetNextFourDaysWeatherForecast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getNextFourDaysWeatherForecast: GetNextFourDaysWeatherForecast,
    private val getForecastDetailsUseCase: GetForecastDetails
) : ViewModel() {

    private val _futureForecastState = mutableStateOf(ForecastListState())
    val futureForecastState: State<ForecastListState> = _futureForecastState

    private val _detailedForecastState = mutableStateOf(DetailedForecastListState())
    val detailedForecastState: State<DetailedForecastListState> = _detailedForecastState

    init {
        getFutureForecast()
        getForecastList()
    }


    private fun getFutureForecast() {
        getNextFourDaysWeatherForecast().onEach { result ->
            when (result) {
                is ResultData.Loading -> _futureForecastState.value =
                    ForecastListState(isLoading = true)
                is ResultData.Success -> _futureForecastState.value =
                    ForecastListState(forecasts = result.data ?: emptyList())
                is ResultData.Failed -> _futureForecastState.value =
                    ForecastListState(error = result.message)
                else -> _futureForecastState.value =
                    ForecastListState(error = UiText.DynamicText("Something went wrong"))
            }
        }.launchIn(viewModelScope)
    }


    fun getForecastList(dateQuery: Int? = 1651752000) { // Default parameter for testing
        dateQuery?.let { date ->
            getForecastDetailsUseCase(date).onEach { result ->
                when (result) {
                    is ResultData.Loading -> _detailedForecastState.value =
                        DetailedForecastListState(isLoading = true)
                    is ResultData.Success -> _detailedForecastState.value =
                        DetailedForecastListState(forecasts = result.data ?: emptyList())
                    is ResultData.Failed -> _detailedForecastState.value =
                        DetailedForecastListState(error = result.message)
                    is ResultData.DoNothing ->
                        DetailedForecastListState(error = UiText.DynamicText("Something went wrong"))
                }
            }.launchIn(viewModelScope)
        }
    }
}