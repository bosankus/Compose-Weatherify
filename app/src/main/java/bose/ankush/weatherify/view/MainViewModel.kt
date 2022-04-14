package bose.ankush.weatherify.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.data.WeatherRepository
import bose.ankush.weatherify.data.model.AvgForecast
import bose.ankush.weatherify.data.model.CurrentTemperature
import bose.ankush.weatherify.util.ResultData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    init {
        fetchClimateDetails()
    }

    fun fetchClimateDetails() {
        getTemperature()
        getWeatherForecast()
    }


    private fun getTemperature() {
        viewModelScope.launch {
            dataSource.getCurrentTemperature().collectLatest { data -> _temperature.value = data }
        }
    }


    private fun getWeatherForecast() {
        viewModelScope.launch {
            dataSource.getWeatherForecast().collectLatest { data -> _forecast.value = data }
        }
    }
}

