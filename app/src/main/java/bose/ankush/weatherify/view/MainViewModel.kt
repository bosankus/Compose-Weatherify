package bose.ankush.weatherify.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**Created by
Author: Ankush Bose
Date: 05,May,2021
 **/

@HiltViewModel
class MainViewModel @Inject constructor(private val dataSource: WeatherRepository) : ViewModel() {

    private var _currentTemp = MutableLiveData<ResultData<*>>(ResultData.DoNothing)
    val currentTemp: LiveData<ResultData<*>> get() = _currentTemp

    private var _weatherForecast =
        MutableLiveData<ResultData<List<AvgForecast>>>(ResultData.DoNothing)
    val weatherForecast: LiveData<ResultData<List<AvgForecast>>>
        get() = _weatherForecast

    private val temperatureExceptionHandler = CoroutineExceptionHandler { _, exception ->
        _currentTemp.postValue(ResultData.Failed("${exception.message}"))
    }

    private val weatherExceptionHandler = CoroutineExceptionHandler { _, exception ->
        _weatherForecast.postValue(ResultData.Failed("${exception.message}"))
    }

    init {
        getCurrentTemperature()
    }


    fun getCurrentTemperature() {
        _currentTemp.postValue(ResultData.Loading)
        viewModelScope.launch(temperatureExceptionHandler) {
            val response: CurrentTemperature? = dataSource.getCurrentTemperature()
            response?.let { _currentTemp.postValue(ResultData.Success(it)) }
                ?: _currentTemp.postValue(ResultData.Failed("Couldn't fetch temperature"))
        }
        getWeatherForecast()
    }


    private fun getWeatherForecast() {
        _weatherForecast.postValue(ResultData.Loading)
        viewModelScope.launch(weatherExceptionHandler) {
            val response: WeatherForecast? = dataSource.getWeatherForecast()
            response?.let { weatherForecast ->
                val forecastList = weatherForecast.list?.getForecastListForNext4Days()
                forecastList?.let { _weatherForecast.postValue(ResultData.Success(it)) }
            }
                ?: _weatherForecast.postValue(ResultData.Failed("Couldn't fetch weather forecast"))

        }
    }
}

