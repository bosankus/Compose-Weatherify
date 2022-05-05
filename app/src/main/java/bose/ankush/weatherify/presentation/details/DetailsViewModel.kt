package bose.ankush.weatherify.presentation.details

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
    private val getForecastsUseCase: GetForecasts
) : ViewModel() {

    private val _state = mutableStateOf(ForecastListState())
    val state: State<ForecastListState> = _state

    private var forecastList = listOf<ForecastDto.ForecastList>()

    init {
        getAllForecasts()
    }

    fun getFourDaysAvgForecast(forecastList: List<ForecastDto.ForecastList>): List<AvgForecast> =
        forecastList.getForecastListForNext4Days()


    fun getDayWiseDetailedForecast(dateQuery: Int?): List<ForecastDto.ForecastList>? =
        dateQuery?.let {
            forecastList.filter {
                it.dt?.let { dates -> getDayNameFromEpoch(dates) } == getDayNameFromEpoch(dateQuery)
            }
        }


    // for loading future forecasts
    private fun getAllForecasts() {
        getForecastsUseCase().onEach { result ->
            when (result) {
                is ResultData.DoNothing -> {}
                is ResultData.Loading -> _state.value = ForecastListState(isLoading = true)
                is ResultData.Success -> {
                    if (result.data?.isNotEmpty() == true) {
                        forecastList = result.data
                        _state.value = ForecastListState(forecasts = result.data)
                    }
                }
                else -> _state.value =
                    ForecastListState(error = UiText.DynamicText("Something went wrong"))
            }
        }.launchIn(viewModelScope)
    }
}