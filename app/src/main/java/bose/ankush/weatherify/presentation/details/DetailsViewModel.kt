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
import bose.ankush.weatherify.data.remote.dto.toCityName
import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.domain.use_case.get_weather_forecasts.GetForecasts
import bose.ankush.weatherify.presentation.details.state.DetailedForecastListState
import bose.ankush.weatherify.presentation.details.state.ForecastListState
import com.bosankus.utilities.DateTimeUtils.getDayNameFromEpoch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    getForecastsUseCase: GetForecasts
) : ViewModel() {

    private val _forecastState = mutableStateOf(ForecastListState())
    val forecastState: State<ForecastListState> = _forecastState

    private val _detailedForecastState = mutableStateOf(DetailedForecastListState())
    val detailedForecastState: State<DetailedForecastListState> = _detailedForecastState

    private val _cityName: MutableState<String?> = mutableStateOf("")
    val cityName: State<String?> = _cityName

    private val _forecastList: MutableState<List<ForecastDto.ForecastList>> =
        mutableStateOf(listOf())

    init {
        getForecastsUseCase().onEach { result ->
            when (result) {
                is ResultData.DoNothing -> {}
                is ResultData.Loading -> _forecastState.value = ForecastListState(isLoading = true)
                is ResultData.Success -> {
                    val responseList = result.data?.list ?: emptyList()
                    _forecastList.value = responseList
                    _cityName.value = result.data?.toCityName()?.name
                    _forecastState.value = ForecastListState(forecasts = responseList)
                }
                else -> _forecastState.value =
                    ForecastListState(error = UiText.DynamicText("Something went wrong"))
            }
        }.launchIn(viewModelScope)
    }


    fun getFourDaysAvgForecast(): List<AvgForecast> {
        return if (_forecastList.value.isNotEmpty()) _forecastList.value.getForecastListForNext4Days()
        else emptyList()
    }


    fun getDayWiseDetailedForecast(dateQuery: Int) {
        try {
            val filteredList = _forecastList.value.filter { list ->
                list.dt?.let { getDayNameFromEpoch(it) == getDayNameFromEpoch(dateQuery) } ?: false
            }
            _detailedForecastState.value = DetailedForecastListState(forecastList = filteredList)
        } catch (e: Exception) {
            _detailedForecastState.value =
                DetailedForecastListState(error = UiText.DynamicText(e.message.toString()))
        }
    }

}