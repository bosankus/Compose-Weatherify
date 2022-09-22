package bose.ankush.weatherify.presentation.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.R
import bose.ankush.weatherify.common.Extension.getForecastListForNext4Days
import bose.ankush.weatherify.common.ResultData
import bose.ankush.weatherify.common.UiText
import bose.ankush.weatherify.data.remote.dto.ForecastDto
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.model.AvgForecast
import bose.ankush.weatherify.domain.model.Weather
import bose.ankush.weatherify.domain.use_case.get_air_quality.GetAirQuality
import bose.ankush.weatherify.domain.use_case.get_weather_forecasts.GetForecasts
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetTodaysWeatherReport
import bose.ankush.weatherify.presentation.UIState
import com.bosankus.utilities.DateTimeUtils.getDayNameFromEpoch
import com.google.android.gms.location.FusedLocationProviderClient
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
    private val getAirQuality: GetAirQuality,
    val fusedLocation: FusedLocationProviderClient
) : ViewModel() {

    private var _todaysWeather = mutableStateOf(UIState<Weather>())
    val todaysWeather: State<UIState<Weather>> = _todaysWeather

    private val _forecastState = mutableStateOf(UIState<List<ForecastDto.ForecastList>>())
    val forecastState: State<UIState<List<ForecastDto.ForecastList>>> = _forecastState

    private val _detailedForecastState = mutableStateOf(listOf<ForecastDto.ForecastList>())
    val detailedForecastState: State<List<ForecastDto.ForecastList>> = _detailedForecastState

    private val _airQuality = mutableStateOf(UIState<AirQuality>())
    val airQuality: State<UIState<AirQuality>> = _airQuality

    private val _cityName: MutableState<String?> = mutableStateOf("")
    val cityName: State<String?> = _cityName

    private val _forecastList: MutableState<List<ForecastDto.ForecastList>> =
        mutableStateOf(listOf())

    /**
     * Fetch weather report from network
     */
    fun fetchWeatherDetails(cityName: String) {
        viewModelScope.launch {
            val todaysWeather = async { getTodaysWeatherUseCase(cityName) }
            val forecast = async { getForecastsUseCase(cityName) }

            todaysWeather.await().onEach { result ->
                when (result) {
                    is ResultData.DoNothing -> {}
                    is ResultData.Loading -> _todaysWeather.value =
                        UIState(isLoading = true)
                    is ResultData.Success -> _todaysWeather.value =
                        UIState(data = result.data)
                    is ResultData.Failed -> _todaysWeather.value =
                        UIState(error = UiText.DynamicText(result.message.toString()))
                }
            }.launchIn(viewModelScope)

            forecast.await().onEach { result ->
                when (result) {
                    is ResultData.DoNothing -> {}
                    is ResultData.Loading -> _forecastState.value =
                        UIState(isLoading = true)
                    is ResultData.Success -> {
                        val responseList = result.data?.list ?: emptyList()
                        _forecastList.value = responseList
                        _cityName.value = result.data?.city?.name
                        _forecastState.value = UIState(data = responseList)
                    }
                    else -> _forecastState.value =
                        UIState(error = UiText.StringResource(R.string.general_error_txt))
                }
            }.launchIn(viewModelScope)
        }
    }


    /**
     * Fetch air quality report from network
     */
    fun fetchAirQuality(lat: Double, lang: Double) {
        viewModelScope.launch {
            getAirQuality(lat, lang).collect { result ->
                when (result) {
                    is ResultData.DoNothing -> {}
                    is ResultData.Loading -> _airQuality.value = UIState(isLoading = true)
                    is ResultData.Success -> {
                        val airQualityReport = result.data
                        if (airQualityReport != null) _airQuality.value = UIState(data = airQualityReport)
                    }
                    is ResultData.Failed -> {
                        _airQuality.value = UIState(error = UiText.DynamicText(result.message.toString()))
                    }
                }
            }
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