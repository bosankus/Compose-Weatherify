package bose.ankush.weatherify.presentation

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.data.preference.PreferenceManager
import bose.ankush.weatherify.data.room.weather.WeatherEntity
import bose.ankush.weatherify.domain.model.AirQuality
import bose.ankush.weatherify.domain.use_case.get_air_quality.GetAirQuality
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetWeatherReport
import bose.ankush.weatherify.domain.use_case.refresh_weather_reports.RefreshWeatherReport
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val client: FusedLocationProviderClient,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    var permissionDialogQueue = mutableStateListOf<String>()
        private set

    private val _weatherReport = MutableStateFlow(UIState<WeatherEntity>())
    val weatherReport = _weatherReport.asStateFlow()

    private val _airQualityReport = MutableStateFlow(UIState<AirQuality>())
    val airQualityReport = _airQualityReport.asStateFlow()

    var userLocationPreference = mutableStateOf(UIState<Pair<Double, Double>>())
        private set

    init {
        performInitialDataLoading()
    }

    fun dismissDialog() {
        permissionDialogQueue.removeFirst()
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean,
    ) {
        if (!isGranted && !permissionDialogQueue.contains(permission)) {
            permissionDialogQueue.add(permission)
        } else {
            fetchAndSaveLocationCoordinates()
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchAndSaveLocationCoordinates() {
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                viewModelScope.launch {
                    val coordinates = Pair(first = location.latitude, second = location.longitude)
                    preferenceManager.saveLocationPreferences(coordinates)
                }
            } else {
                userLocationPreference.value =
                    UIState(error = UiText.DynamicText("Error fetching location data!"))
            }
        }
            .addOnFailureListener { exception ->
                userLocationPreference.value =
                    UIState(error = UiText.DynamicText(exception.message.toString()))
            }
    }


    // initial data loading to get things ready for UI
    fun performInitialDataLoading() {
        viewModelScope.launch {
            /**
             * Get saved weather report.
             * In case there is no data, data will be refreshed as per user's location,
             * and data will flow to compose UI
             */

            // fetch weather report from GetWeatherReport use case (from ROOM DB)
            getWeatherReport().collect { result -> _weatherReport.value = result }

            // considering we have current location coordinates, we are fetching that from storage preference
            val preferences = preferenceManager.getLocationPreferenceFlow().first()
            val latitude = preferences[PreferenceManager.USER_LAT_LOCATION]
            val longitude = preferences[PreferenceManager.USER_LON_LOCATION]
            if (latitude != null && longitude != null) {
                val location = Pair(latitude, longitude)

                // update UI state
                userLocationPreference.value = UIState(data = location)

                // fetch and save weather report from remote to ROOM DB
                refreshWeatherReport(location)

                // fetch air quality from GetAirQuality use case (from remote)
                getAirQuality(location.first, location.second).collect { result ->
                    _airQualityReport.value = result
                }
            } else {
                userLocationPreference.value =
                    UIState(error = UiText.DynamicText("No location coordinates"))
            }
        }
    }
}
