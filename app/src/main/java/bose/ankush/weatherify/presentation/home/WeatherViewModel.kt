package bose.ankush.weatherify.presentation.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.data.preference.PreferenceManager
import bose.ankush.weatherify.data.room.WeatherEntity
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.use_case.get_air_quality.GetAirQuality
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetWeatherReport
import bose.ankush.weatherify.domain.use_case.refresh_weather_reports.RefreshWeatherReport
import bose.ankush.weatherify.presentation.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**Created by
Author: Ankush Bose
Date: 05,May,2021
Updated: 09,July,2023
 **/

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    private val _weatherReport = MutableStateFlow(UIState<WeatherEntity>())
    val weatherReport = _weatherReport.asStateFlow()

    private val _airQualityReport = MutableStateFlow(UIState<AirQuality>())
    val airQualityReport = _airQualityReport.asStateFlow()

    var userLocationPreference = mutableStateOf(UIState<Pair<Double, Double>>())
        private set

    init {
        performInitialDataLoading()
    }

    // initial data loading to get things ready for UI
    fun performInitialDataLoading() {
        viewModelScope.launch {
            // Get saved weather report.
            // In case there is no data, in L84 data will be refreshed as per user's location,
            // and data will flow to compose UI
            fetchWeatherReport()

            // considering we have current location coordinates, we are fetching that from storage preference
            preferenceManager.getLocationPreferenceFlow()
                .catch { e ->
                    userLocationPreference.value =
                        UIState(error = UiText.DynamicText(e.message.toString()))
                }
                .collectLatest {
                    val location: Pair<Double, Double> = Pair(
                        it[PreferenceManager.USER_LAT_LOCATION] ?: 0.0,
                        it[PreferenceManager.USER_LON_LOCATION] ?: 0.0
                    )
                    // TODO: check if ('new location' - 'saved location') = X meters away
                    // hold value for UI reference
                    userLocationPreference.value = UIState(data = location)
                    refreshWeatherReportInRoom(location)
                    fetchAirQualityReport(location)
                }
        }
    }

    // fetch and save weather report from remote to ROOM DB
    private fun refreshWeatherReportInRoom(location: Pair<Double, Double>) {
        viewModelScope.launch { refreshWeatherReport(location) }
    }

    // fetch weather report from GetWeatherReport use case (from ROOM DB)
    private fun fetchWeatherReport() {
        viewModelScope.launch {
            getWeatherReport().collect { result -> _weatherReport.value = result }
        }
    }

    // fetch air quality from GetAirQuality use case (from remote)
    private fun fetchAirQualityReport(location: Pair<Double, Double>) {
        viewModelScope.launch {
            getAirQuality(location.first, location.second).collect { result ->
                _airQualityReport.value = result
            }
        }
    }
}
