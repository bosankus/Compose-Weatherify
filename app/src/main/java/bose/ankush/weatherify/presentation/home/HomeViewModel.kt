package bose.ankush.weatherify.presentation.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.DEFAULT_CITY_NAME
import bose.ankush.weatherify.common.Extension.getForecastListForNext4Days
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.domain.use_case.get_weather_forecasts.GetForecasts
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetTodaysWeatherReport
import bose.ankush.weatherify.presentation.home.state.ForecastListState
import bose.ankush.weatherify.presentation.home.state.TodaysWeatherState
import com.bosankus.utilities.DateTimeUtils.getDayNameFromEpoch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodaysWeatherUseCase: GetTodaysWeatherReport,
    private val getForecastsUseCase: GetForecasts,
) : ViewModel() {

    private var _todaysWeather = mutableStateOf(TodaysWeatherState())
    val todaysWeather: State<TodaysWeatherState> = _todaysWeather

    private val _forecastState = mutableStateOf(ForecastListState())
    val forecastState: State<ForecastListState> = _forecastState

    private val _detailedForecastState = mutableStateOf(listOf<ForecastDto.ForecastList>())
    val detailedForecastState: State<List<ForecastDto.ForecastList>> = _detailedForecastState

    private val _cityName: MutableState<String?> = mutableStateOf("")
    val cityName: State<String?> = _cityName

    private val _forecastList: MutableState<List<ForecastDto.ForecastList>> =
        mutableStateOf(listOf())

    /*init {
        fetchWeatherDetails(DEFAULT_CITY_NAME)
    }*/


    fun fetchWeatherDetails(cityName: String) {
        viewModelScope.launch {
            val todaysWeather = async { getTodaysWeatherUseCase(cityName) }
            val forecast = async { getForecastsUseCase(cityName) }

            todaysWeather.await().onEach { result ->
                when (result) {
                    is ResultData.DoNothing -> {}
                    is ResultData.Loading -> _todaysWeather.value =
                        TodaysWeatherState(isLoading = true)
                    is ResultData.Success -> _todaysWeather.value =
                        TodaysWeatherState(weather = result.data)
                    is ResultData.Failed -> _todaysWeather.value =
                        TodaysWeatherState(error = UiText.DynamicText(result.message.toString()))
                }
            }.launchIn(viewModelScope)

            forecast.await().onEach { result ->
                when (result) {
                    is ResultData.DoNothing -> {}
                    is ResultData.Loading -> _forecastState.value =
                        ForecastListState(isLoading = true)
                    is ResultData.Success -> {
                        val responseList = result.data?.list ?: emptyList()
                        _forecastList.value = responseList
                        _cityName.value = result.data?.city?.name
                        _forecastState.value = ForecastListState(forecasts = responseList)
                    }
                    else -> _forecastState.value =
                        ForecastListState(error = UiText.StringResource(R.string.general_error_txt))
                }
            }.launchIn(viewModelScope)
        }
    }


    fun getFourDaysAvgForecast(): List<AvgForecast> {
        return if (_forecastList.value.isNotEmpty()) _forecastList.value.getForecastListForNext4Days()
        else emptyList()
    }


    fun getDayWiseDetailedForecast(dateQuery: Int) {
        _detailedForecastState.value = _forecastList.value.filter { list ->
            list.dt?.let { getDayNameFromEpoch(it) == getDayNameFromEpoch(dateQuery) } ?: false
        }
    }

}