package bose.ankush.weatherify.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.data.WeatherRepository
import bose.ankush.weatherify.data.model.AvgForecast
import bose.ankush.weatherify.data.model.CurrentTemperature
import bose.ankush.weatherify.data.model.WeatherForecast
import bose.ankush.weatherify.util.Extension.getForecastListForNext4Days
import bose.ankush.weatherify.util.ResultData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

@HiltViewModel
class MainViewModel @Inject constructor(private val dataSource: WeatherRepository) : ViewModel() {

    private var _temperature =
        MutableStateFlow<ResultData<CurrentTemperature>>(ResultData.DoNothing)
    val temperature get() = _temperature.asStateFlow()

    private var _forecast = MutableStateFlow<ResultData<List<AvgForecast>>>(ResultData.DoNothing)
    val forecast = _forecast.asStateFlow()

    private val temperatureExceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch { _temperature.emit(ResultData.Failed("${exception.message}")) }
    }

    private val weatherExceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch { _forecast.emit(ResultData.Failed("${exception.message}")) }
    }

    init {
        fetchClimateDetails()
    }

    fun fetchClimateDetails() {
        getCurrentTemperature()
        getWeatherForecast()
    }

    fun getCurrentTemperature() {
        viewModelScope.launch(temperatureExceptionHandler) {
            _temperature.value = ResultData.Loading
            val response: CurrentTemperature? = dataSource.getCurrentTemperature()
            _temperature.value = response?.let { ResultData.Success(it) }
                ?: ResultData.Failed("Couldn't fetch temperature")
        }
    }


    private fun getWeatherForecast() {
        viewModelScope.launch(weatherExceptionHandler) {
            _forecast.emit(ResultData.Loading)
            val response: WeatherForecast? = dataSource.getWeatherForecast()
            response?.let { weatherForecast ->
                val forecastList = weatherForecast.list?.getForecastListForNext4Days()
                forecastList?.let { _forecast.emit(ResultData.Success(it)) }
            }
                ?: _forecast.emit(ResultData.Failed("Couldn't fetch weather forecast"))

        }
    }
}

