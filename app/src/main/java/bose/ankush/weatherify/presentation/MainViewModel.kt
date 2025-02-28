package bose.ankush.weatherify.presentation

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.ENABLE_NOTIFICATION
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.data.preference.PreferenceManager
import bose.ankush.weatherify.domain.use_case.get_air_quality.GetAirQuality
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetWeatherReport
import bose.ankush.weatherify.domain.use_case.refresh_weather_reports.RefreshWeatherReport
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val locationProviderClient: FusedLocationProviderClient,
    private val preferenceManager: PreferenceManager,
    dispatchers: DispatcherProvider
) : ViewModel() {

    var permissionDialogQueue = mutableStateListOf<String>()
        private set

    private val _uiState = MutableStateFlow(UIState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _launchPhoneCallPermission = MutableStateFlow(false)
    val launchPhoneCallPermission = _launchPhoneCallPermission.asStateFlow()

    private val _launchNotificationPermission = MutableStateFlow(false)
    val launchNotificationPermission = _launchNotificationPermission.asStateFlow()

    private val _showNotificationCardItem = MutableStateFlow(false)
    val showNotificationCardItem = _showNotificationCardItem.asStateFlow()

    private val dataFetchExceptionHandler = CoroutineExceptionHandler { _, e ->
        _uiState.update { UIState(error = UiText.DynamicText(e.message.toString())) }
    } + dispatchers.io

    private val remoteConfig = Firebase.remoteConfig
    private val tag = "${MainViewModel::class.simpleName} ->"

    init {
        updateRemoteConfigParameters()
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

    fun updatePhoneCallPermission(launchState: Boolean) {
        _launchPhoneCallPermission.update { launchState }
    }

    fun updateNotificationPermission(launchState: Boolean) {
        _launchNotificationPermission.update { launchState }
    }

    /**
     * Updates the state of the notification banner based on the remote configuration.
     * If notifications are disabled, the banner visibility will be false.
     */
    fun updateShowNotificationBannerState(launchState: Boolean) {
        viewModelScope.launch(dataFetchExceptionHandler) {
            if (remoteConfig.getBoolean(ENABLE_NOTIFICATION)) {
                _showNotificationCardItem.update { launchState }
            } else {
                _showNotificationCardItem.update { false }
                Timber.tag(tag).d("Notification feature is disabled. Flow won't update.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchAndSaveLocationCoordinates() {
        locationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                viewModelScope.launch(dataFetchExceptionHandler) {
                    val coordinates = Pair(first = location.latitude, second = location.longitude)
                    // storing location on shared preference
                    preferenceManager.saveLocationPreferences(coordinates)
                    // load initial data when coordinates received
                    performInitialDataLoading()
                }
            }
        }
            .addOnFailureListener { e -> throw RuntimeException(e.message.toString()) }
    }


    // initial data loading to get things ready for UI
    private fun performInitialDataLoading() {
        viewModelScope.launch(dataFetchExceptionHandler) {
            // Get coordinates from preference
            val preferences = preferenceManager.getLocationPreferenceFlow().first()
            val latitude = preferences[PreferenceManager.USER_LAT_LOCATION]
            val longitude = preferences[PreferenceManager.USER_LON_LOCATION]

            if (latitude != null && longitude != null) {
                val location = Pair(latitude, longitude)

                // fetch and save weather report from remote to ROOM DB
                refreshWeatherReport(location)

                // zip both data streams and collect to populate on UI state data class.
                // Also update UI state about user's location coordinates
                getAirQuality(location.first, location.second)
                    .combine(getWeatherReport.invoke(location)) { air, weather ->
                        UIState(
                            isLoading = false,
                            userLocation = location,
                            weatherData = weather,
                            airQualityData = air,
                            error = null
                        )
                    }.collect { newState -> _uiState.update { newState } }
            } else {
                // in case we don't have coordinates, we don't have any requirement yet other than this :(
                _uiState.update { UIState(isLoading = false) }
                throw RuntimeException("No location coordinates")
            }
        }
    }

    // Update remote config parameters
    private fun updateRemoteConfigParameters() {
        viewModelScope.launch(dataFetchExceptionHandler) {
            val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 }
            remoteConfig.apply {
                setConfigSettingsAsync(configSettings)
                setDefaultsAsync(R.xml.remote_config_defaults)
                fetchAndActivate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Timber.tag(tag).d("Remote config parameters updated.")
                    } else {
                        Timber.tag(tag).d("Failed to update Remote Config parameters.")
                    }
                }
            }
        }
    }
}
