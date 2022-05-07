package bose.ankush.weatherify.presentation.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.common.Extension.getForecastListForNext4Days
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.domain.use_case.get_weather_forecasts.GetForecasts
import com.bosankus.utilities.DateTimeUtils.getDayNameFromEpoch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    getForecastsUseCase: GetForecasts
) : ViewModel() {

    private val _state = mutableStateOf(ForecastListState())
    val state: State<ForecastListState> = _state

    private val _detailedForecastState = mutableStateOf(DetailedForecastListState())
    val detailedForecastState: State<DetailedForecastListState> = _detailedForecastState

    var forecastList: MutableState<List<ForecastDto.ForecastList>> = mutableStateOf(listOf())

    init {
        getForecastsUseCase().onEach { result ->
            when (result) {
                is ResultData.DoNothing -> {}
                is ResultData.Loading -> _state.value = ForecastListState(isLoading = true)
                is ResultData.Success -> {
                    if (result.data?.isNotEmpty() == true) {
                        forecastList.value = result.data
                        _state.value = ForecastListState(forecasts = result.data)
                    }
                }
                else -> _state.value =
                    ForecastListState(error = UiText.DynamicText("Something went wrong"))
            }
        }.launchIn(viewModelScope)
    }


    fun getFourDaysAvgForecast(): List<AvgForecast> {
        return if (forecastList.value.isNotEmpty()) forecastList.value.getForecastListForNext4Days()
        else emptyList()
    }


    fun getDayWiseDetailedForecast(dateQuery: Int) {
        try {
            val filteredList = forecastList.value.filter { list ->
                list.dt?.let { getDayNameFromEpoch(it) == getDayNameFromEpoch(dateQuery) } ?: false
            }
            _detailedForecastState.value = DetailedForecastListState(forecastList = filteredList)
        } catch (e: Exception) {
            _detailedForecastState.value =
                DetailedForecastListState(error = UiText.DynamicText(e.message.toString()))
        }
    }

}