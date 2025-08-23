package bose.ankush.weatherify.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.ENABLE_NOTIFICATION
import bose.ankush.weatherify.base.common.Extension
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.base.common.errorResponseFromException
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Main ViewModel for the Weatherify application.
 * 
 * This ViewModel is responsible for:
 * - Managing the UI state for weather and air quality data
 * - Handling location permissions and coordinates
 * - Managing notification settings and permissions
 * - Coordinating data loading from repositories
 *
 * @property refreshWeatherReport Use case for refreshing weather data from remote source
 * @property getWeatherReport Use case for retrieving weather data from local database
 * @property getAirQuality Use case for retrieving air quality data
 * @property locationClient Client for accessing device location
 * @property preferenceManager Manager for user preferences storage
 * @property dispatchers Provider for coroutine dispatchers
 * @property remoteConfigService Service for accessing remote configuration
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val locationClient: LocationClient,
    private val preferenceManager: PreferenceManager,
    private val dispatchers: DispatcherProvider,
    private val remoteConfigService: RemoteConfigService,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Queue of permissions that need to be requested from the user.
     * This is exposed to the UI to show appropriate permission dialogs.
     */
    var permissionDialogQueue = mutableStateListOf<String>()
        private set

    private val _uiState = MutableStateFlow(UIState(isLoading = true))
    /**
     * The current UI state containing weather data, air quality, and loading status.
     */
    val uiState = _uiState.asStateFlow()

    private val _launchNotificationPermission = MutableStateFlow(false)
    /**
     * Flag indicating whether the notification permission dialog should be shown.
     */
    val launchNotificationPermission = _launchNotificationPermission.asStateFlow()

    private val _showNotificationCardItem = MutableStateFlow(false)
    /**
     * Flag indicating whether the notification card should be shown in the UI.
     */
    val showNotificationCardItem = _showNotificationCardItem.asStateFlow()

    // Authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)

    /**
     * The current authentication state.
     */
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Login status
    private val _isLoggedIn = MutableStateFlow(false)

    /**
     * Flag indicating whether the user is logged in.
     */
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Authentication initialization flag to avoid UI flicker on app start
    private val _isAuthInitialized = MutableStateFlow(false)

    /**
     * True when the initial authentication check has completed.
     */
    val isAuthInitialized: StateFlow<Boolean> = _isAuthInitialized.asStateFlow()

    init {
        // Check if user is already logged in and mark auth initialization after first emission
        viewModelScope.launch {
            var hasInitialized = false
            authRepository.isLoggedIn().collectLatest { isLoggedIn ->
                _isLoggedIn.value = isLoggedIn
                if (!hasInitialized) {
                    _isAuthInitialized.value = true
                    hasInitialized = true
                }
            }
        }
    }

    /**
     * Exception handler for data fetching operations.
     * Updates the UI state with an error message when an exception occurs.
     */
    private val dataFetchExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e !is CancellationException) {
            // Cast Throwable to Exception if possible, otherwise use a generic error message
            val error = if (e is Exception) {
                errorResponseFromException(e)
            } else {
                UiText.StringResource(resId = R.string.general_error_txt)
            }
            _uiState.update { UIState(error = error) }
        }
    }

    private val tag = "${MainViewModel::class.simpleName} ->"

    // Track active jobs for proper cancellation
    private var notificationBannerJob: Job? = null
    private var locationJob: Job? = null
    private var dataLoadingJob: Job? = null

    /**
     * Dismisses the current permission dialog by removing it from the queue.
     * This should be called when the user has responded to a permission request.
     */
    fun dismissDialog() {
        permissionDialogQueue.removeAt(0)
    }

    /**
     * Handles the result of a permission request.
     * If permission is denied, adds it to the dialog queue to show a rationale.
     * If permission is granted, proceeds with fetching location coordinates.
     *
     * @param permission The permission that was requested
     * @param isGranted Whether the permission was granted by the user
     */
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

    /**
     * Updates the state of the notification permission dialog.
     *
     * @param launchState True to show the permission dialog, false to hide it
     */
    fun updateNotificationPermission(launchState: Boolean) {
        _launchNotificationPermission.update { launchState }
    }

    /**
     * Updates the state of the notification banner based on the remote configuration.
     * If notifications are disabled, the banner visibility will be false.
     *
     * @param launchState True to show the notification banner if enabled in remote config, false to hide it
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
                _uiState.update { it.copy(error = errorResponseFromException(e)) }
            }
        }
    }

    /**
     * Fetches the user's current location coordinates and saves them to preferences.
     * Once coordinates are obtained, triggers initial data loading for weather and air quality.
     * This method handles errors and updates the UI state accordingly.
     */
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
                        val error = if (e is Exception) {
                            errorResponseFromException(e)
                        } else {
                            UiText.StringResource(resId = R.string.general_error_txt)
                        }
                        _uiState.update { UIState(error = error) }
                    }
                )
            } catch (e: CancellationException) {
                throw e // Rethrow cancellation exceptions
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error fetching location coordinates")
                _uiState.update { it.copy(error = errorResponseFromException(e)) }
            }
        }
    }


    /**
     * Performs initial data loading to prepare weather and air quality data for the UI.
     * 
     * This method:
     * 1. Retrieves user location coordinates from preferences
     * 2. Refreshes weather data from remote source and saves to local database
     * 3. Combines air quality and weather data streams
     * 4. Updates the UI state with the combined data
     * 
     * The method handles various error cases:
     * - Missing coordinates
     * - Network errors
     * - Data processing errors
     */
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
                            val error = if (e is Exception) {
                                errorResponseFromException(e)
                            } else {
                                UiText.StringResource(resId = R.string.general_error_txt)
                            }
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = error
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
                        error = errorResponseFromException(e)
                    ) 
                }
            }
        }
    }

    /**
     * Login with email and password
     * @param email User's email
     * @param password User's password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = authRepository.login(email, password)
                handleAuthResponse(response)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(UiText.DynamicText(e.message ?: "Login failed"))
            }
        }
    }

    /**
     * Register with email and password
     * @param email User's email
     * @param password User's password
     */
    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Collect device information
                val timestamp = Extension.getCurrentUtcTimestamp()
                val deviceModel = Extension.getDeviceModel()
                val operatingSystem = Extension.getOperatingSystem()
                val osVersion = Extension.getOsVersion()
                val appVersion = Extension.getAppVersion()
                val ipAddress = Extension.getIpAddress()
                val registrationSource = Extension.getRegistrationSource()
                val firebaseToken = Extension.getFirebaseToken()

                // Call repository with enhanced data
                val response = authRepository.register(
                    email = email,
                    password = password,
                    timestampOfRegistration = timestamp,
                    deviceModel = deviceModel,
                    operatingSystem = operatingSystem,
                    osVersion = osVersion,
                    appVersion = appVersion,
                    ipAddress = ipAddress,
                    registrationSource = registrationSource,
                    firebaseToken = firebaseToken
                )
                handleAuthResponse(response)
            } catch (e: Exception) {
                _authState.value =
                    AuthState.Error(UiText.DynamicText(e.message ?: "Registration failed"))
            }
        }
    }

    /**
     * Logout the user
     */
    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.LogoutLoading
            try {
                val result = authRepository.logout()
                if (result.isSuccess) {
                    _authState.value = AuthState.LoggedOut
                } else {
                    _authState.value = AuthState.Error(
                        UiText.DynamicText(
                            result.exceptionOrNull()?.message ?: "Logout failed"
                        )
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(UiText.DynamicText(e.message ?: "Logout failed"))
            }
        }
    }

    /**
     * Handle authentication response
     * @param response The authentication response
     */
    private fun handleAuthResponse(response: AuthResponse) {
        val token = response.data?.token
        if (response.isSuccess() && token != null && token.isNotBlank()) {
            _authState.value = AuthState.Success
        } else {
            _authState.value = AuthState.Error(
                UiText.DynamicText(response.message ?: "Authentication failed")
            )
        }
    }

    /**
     * Reset authentication state to initial
     */
    fun resetAuthState() {
        _authState.value = AuthState.Initial
    }

    /**
     * Cleans up resources when the ViewModel is cleared.
     * Cancels all active coroutine jobs to prevent memory leaks and unnecessary work.
     */
    override fun onCleared() {
        super.onCleared()
        // Cancel all active jobs when ViewModel is cleared
        notificationBannerJob?.cancel()
        locationJob?.cancel()
        dataLoadingJob?.cancel()
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object LogoutLoading : AuthState()
    object Success : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: UiText) : AuthState()
}
