package bose.ankush.weatherify.presentation.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.DateTimeUtils.getDayNameFromEpoch
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
Updated: 09,July,2023
 **/

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodaysWeatherUseCase: GetTodaysWeatherReport,
    private val getForecastsUseCase: GetForecasts,
    private val getAirQuality: GetAirQuality,
    val fusedLocationProviderClient: FusedLocationProviderClient,
) : ViewModel() {

    /*For Home Screen*/

    var todayWeather = mutableStateOf(UIState<Weather>())
        private set

    var forecastState = mutableStateOf(UIState<List<ForecastDto.ForecastList>>())
        private set

    var detailedForecastState = mutableStateOf(listOf<ForecastDto.ForecastList>())
        private set

    var airQuality = mutableStateOf(UIState<AirQuality>())
        private set

    var cityName: MutableState<String?> = mutableStateOf("")
        private set

    private val forecastList: MutableState<List<ForecastDto.ForecastList>> =
        mutableStateOf(listOf())

    /*For City List Screen*/
    // Refactor here details from city list view model

    fun fetchAirQuality(lat: Double, lang: Double) {
        viewModelScope.launch {
            getAirQuality(lat, lang).collect { result ->
                when (result) {
                    is ResultData.DoNothing -> {}
                    is ResultData.Loading -> airQuality.value = UIState(isLoading = true)
                    is ResultData.Success -> {
                        val airQualityReport = result.data
                        if (airQualityReport != null) airQuality.value =
                            UIState(data = airQualityReport)
                    }

                    is ResultData.Failed -> {
                        airQuality.value =
                            UIState(error = UiText.DynamicText(result.message.toString()))
                    }
                }
            }
        }
    }

    fun fetchWeatherDetails(city: String) {
        viewModelScope.launch {
            val cityWeather = async { getTodaysWeatherUseCase(city) }
            val forecast = async { getForecastsUseCase(city) }

            cityWeather.await().onEach { result ->
                when (result) {
                    is ResultData.DoNothing -> {}
                    is ResultData.Loading -> todayWeather.value =
                        UIState(isLoading = true)

                    is ResultData.Success -> todayWeather.value =
                        UIState(data = result.data)

                    is ResultData.Failed -> todayWeather.value =
                        UIState(error = UiText.DynamicText(result.message.toString()))
                }
            }.launchIn(viewModelScope)

            forecast.await().onEach { result ->
                when (result) {
                    is ResultData.DoNothing -> {}
                    is ResultData.Loading -> forecastState.value =
                        UIState(isLoading = true)

                    is ResultData.Success -> {
                        val responseList = result.data?.list ?: emptyList()
                        forecastList.value = responseList
                        cityName.value = result.data?.city?.name
                        forecastState.value = UIState(data = responseList)
                    }

                    else -> forecastState.value =
                        UIState(error = UiText.StringResource(R.string.general_error_txt))
                }
            }.launchIn(viewModelScope)
        }
    }


    fun getFourDaysAvgForecast(): List<AvgForecast> {
        return if (forecastList.value.isNotEmpty()) forecastList.value.getForecastListForNext4Days()
        else emptyList()
    }


    fun getDayWiseDetailedForecast(dateQuery: Int) {
        detailedForecastState.value = forecastList.value.filter { list ->
            list.dt?.let { getDayNameFromEpoch(it) == getDayNameFromEpoch(dateQuery) } ?: false
        }
    }

}
