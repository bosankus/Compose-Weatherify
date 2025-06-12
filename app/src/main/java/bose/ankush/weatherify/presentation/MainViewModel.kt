package bose.ankush.weatherify.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.ENABLE_NOTIFICATION
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.domain.preference.PreferenceManager
import bose.ankush.weatherify.domain.remote_config.RemoteConfigService
import bose.ankush.weatherify.domain.use_case.get_air_quality.GetAirQuality
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetWeatherReport
import bose.ankush.weatherify.domain.use_case.refresh_weather_reports.RefreshWeatherReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val locationClient: LocationClient,
    private val preferenceManager: PreferenceManager,
    private val dispatchers: DispatcherProvider,
    private val remoteConfigService: RemoteConfigService
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
        if (e !is CancellationException) {
            _uiState.update { UIState(error = UiText.DynamicText(e.message.toString())) }
        }
    }

    private val tag = "${MainViewModel::class.simpleName} ->"

    // Track active jobs for proper cancellation
    private var notificationBannerJob: Job? = null
    private var locationJob: Job? = null
    private var dataLoadingJob: Job? = null

    fun dismissDialog() {
        permissionDialogQueue.removeAt(0)
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
        // Cancel previous job if it exists
        notificationBannerJob?.cancel()

        notificationBannerJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                if (remoteConfigService.getBoolean(ENABLE_NOTIFICATION)) {
                    _showNotificationCardItem.update { launchState }
                    Timber.tag(tag).d("Notification feature is enabled")
                } else {
                    _showNotificationCardItem.update { false }
                    Timber.tag(tag).d("Notification feature is disabled")
                }
            } catch (e: CancellationException) {
                throw e // Rethrow cancellation exceptions
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error updating notification banner state")
                _uiState.update { it.copy(error = UiText.DynamicText(e.message.toString())) }
            }
        }
    }

    fun fetchAndSaveLocationCoordinates() {
        // Cancel previous job if it exists
        locationJob?.cancel()

        locationJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                locationClient.getCurrentLocation().fold(
                    onSuccess = { location ->
                        val coordinates = Pair(first = location.latitude, second = location.longitude)
                        // storing location on shared preference
                        preferenceManager.saveLocationPreferences(coordinates)
                        // load initial data when coordinates received
                        performInitialDataLoading()
                    },
                    onFailure = { e ->
                        _uiState.update { UIState(error = UiText.DynamicText(e.message.toString())) }
                    }
                )
            } catch (e: CancellationException) {
                throw e // Rethrow cancellation exceptions
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error fetching location coordinates")
                _uiState.update { it.copy(error = UiText.DynamicText(e.message.toString())) }
            }
        }
    }


    // initial data loading to get things ready for UI
    private fun performInitialDataLoading() {
        // Cancel previous job if it exists
        dataLoadingJob?.cancel()

        dataLoadingJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
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
                        }
                        .flowOn(dispatchers.io)
                        .catch { e ->
                            if (e is CancellationException) throw e
                            Timber.tag(tag).e(e, "Error loading weather data")
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = UiText.DynamicText(e.message.toString())
                                ) 
                            }
                        }
                        .onEach { newState -> _uiState.update { newState } }
                        .launchIn(this)
                } else {
                    // in case we don't have coordinates, update UI state with appropriate error message
                    _uiState.update { 
                        UIState(
                            isLoading = false, 
                            error = UiText.StringResource(R.string.default_coordinates_txt)
                        ) 
                    }
                }
            } catch (e: CancellationException) {
                throw e // Rethrow cancellation exceptions
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error in initial data loading")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = UiText.DynamicText(e.message.toString())
                    ) 
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel all active jobs when ViewModel is cleared
        notificationBannerJob?.cancel()
        locationJob?.cancel()
        dataLoadingJob?.cancel()
    }
}
