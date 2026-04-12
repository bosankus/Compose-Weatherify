package bose.ankush.weatherify.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.network.auth.events.AuthEvent
import bose.ankush.network.auth.events.AuthEventBus.emit
import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.auth.token.TokenResult
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.DeviceInfoProvider
import bose.ankush.weatherify.base.common.LoggerFactory
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.base.common.errorResponseFromException
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.base.location.LocationPermissions
import bose.ankush.weatherify.base.common.ENABLE_NOTIFICATION
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Main ViewModel for Weatherify.
 * Handles UI state, authentication, location, and notifications.
 *
 * Payment state and logic lives in [bose.ankush.payment.presentation.PaymentViewModel].
 *
 * All platform-specific dependencies are injected via interfaces so this class
 * is ready to be moved to a KMP commonMain source set with minimal changes.
 *
 * Remaining KMP TODO: [UiText.StringResource] still references Android R.string resources.
 * When migrating UiText to a KMP-compatible text-resource system, replace the
 * [UiText.StringResource] usages below with the new type.
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
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    loggerFactory: LoggerFactory,
    private val deviceInfoProvider: DeviceInfoProvider,
) : ViewModel() {

    private val logger = loggerFactory.create("${MainViewModel::class.simpleName} ->")

    // Permission dialog queue for UI
    var permissionDialogQueue = mutableStateListOf<String>()
        private set

    // UI state flows
    private val _uiState = MutableStateFlow(UIState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _launchNotificationPermission = MutableStateFlow(false)
    val launchNotificationPermission = _launchNotificationPermission.asStateFlow()

    private val _showNotificationCardItem = MutableStateFlow(false)
    val showNotificationCardItem = _showNotificationCardItem.asStateFlow()

    // Auth state flows
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAuthInitialized = MutableStateFlow(false)
    val isAuthInitialized: StateFlow<Boolean> = _isAuthInitialized.asStateFlow()

    // Coroutine jobs
    private var notificationBannerJob: Job? = null
    private var locationJob: Job? = null
    private var dataLoadingJob: Job? = null

    // Exception handler for data fetch
    private val dataFetchExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e !is CancellationException) {
            val error = if (e is Exception) errorResponseFromException(e)
            else UiText.StringResource(resId = R.string.general_error_txt)
            _uiState.update { UIState(error = error) }
        }
    }

    init {
        logger.d("MainViewModel initialized")

        viewModelScope.launch {
            var initialized = false
            authRepository.isLoggedIn().collectLatest { loggedIn ->
                _isLoggedIn.value = loggedIn
                logger.d("Auth state changed - isLoggedIn: $loggedIn")
                if (!initialized) {
                    _isAuthInitialized.value = true
                    initialized = true
                    logger.d("Auth initialization completed")
                    if (loggedIn) silentTokenRefresh()
                }
            }
        }
    }

    /** Remove first permission dialog from queue. */
    fun dismissDialog() {
        if (permissionDialogQueue.isNotEmpty()) {
            val dismissed = permissionDialogQueue.removeAt(0)
            logger.d("Dismissed permission dialog: $dismissed")
        }
    }

    /** Remove all permissions from the queue that have been granted (e.g. via system Settings).
     *  Also triggers location fetch if a location permission was among those granted. */
    fun removeGrantedPermissions(grantedPermissions: List<String>) {
        var locationGranted = false
        grantedPermissions.forEach { permission ->
            permissionDialogQueue.remove(permission)
            logger.d("Removed granted permission from queue: $permission")
            if (permission == LocationPermissions.FINE_LOCATION ||
                permission == LocationPermissions.COARSE_LOCATION
            ) {
                locationGranted = true
            }
        }
        if (locationGranted) {
            logger.d("Location permission granted via Settings, fetching location")
            fetchAndSaveLocationCoordinates()
        }
    }

    /** Handle permission result, fetch location if granted. */
    fun onPermissionResult(permission: String, isGranted: Boolean) {
        logger.d("Permission result - permission: $permission, granted: $isGranted")
        if (isGranted) {
            logger.d("Permission granted, fetching location")
            fetchAndSaveLocationCoordinates()
        } else if (!permissionDialogQueue.contains(permission)) {
            permissionDialogQueue.add(permission)
            logger.w("Permission denied: $permission, added to queue")
        }
    }

    /** Show/hide notification permission dialog. */
    fun updateNotificationPermission(launchState: Boolean) {
        logger.d("Updating notification permission dialog - show: $launchState")
        _launchNotificationPermission.update { launchState }
    }

    /** Show/hide notification banner based on remote config. */
    fun updateShowNotificationBannerState(launchState: Boolean) {
        notificationBannerJob?.cancel()
        notificationBannerJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                val enabled = remoteConfigService.getBoolean(ENABLE_NOTIFICATION)
                _showNotificationCardItem.update { enabled && launchState }
                logger.d("Notification feature is ${if (enabled) "enabled" else "disabled"}")
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (e: Exception) {
                logger.e("Error updating notification banner state", e)
                _uiState.update { it.copy(error = errorResponseFromException(e)) }
            }
        }
    }

    /** Fetch and save user location, then load initial data. */
    fun fetchAndSaveLocationCoordinates() {
        logger.d("Starting location fetch")
        _uiState.update { UIState(isLoading = true) }
        locationJob?.cancel()
        locationJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                locationClient.getCurrentLocation().fold(
                    onSuccess = { loc ->
                        logger.i("Location fetched successfully - lat: ${loc.latitude}, lon: ${loc.longitude}")
                        preferenceManager.saveLocationPreferences(loc.latitude to loc.longitude)
                        logger.d("Location preferences saved")
                        performInitialDataLoading()
                    },
                    onFailure = { e ->
                        logger.e("Location fetch failed", e)
                        val isGpsDisabled = e is LocationClient.LocationException &&
                                e.message?.contains("GPS is disabled", ignoreCase = true) == true
                        val error = if (isGpsDisabled)
                            UiText.StringResource(resId = R.string.gps_disabled_error_txt)
                        else if (e is Exception) errorResponseFromException(e)
                        else UiText.StringResource(resId = R.string.general_error_txt)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error,
                                isGpsDisabled = isGpsDisabled
                            )
                        }
                    }
                )
            } catch (_: CancellationException) {
                logger.d("Location fetch cancelled")
            } catch (e: Exception) {
                logger.e("Error fetching location coordinates", e)
                _uiState.update {
                    it.copy(isLoading = false, error = errorResponseFromException(e))
                }
            }
        }
    }

    /** Refresh weather data without clearing the existing UI (pull-to-refresh). */
    fun refreshWeatherData() {
        logger.d("Starting pull-to-refresh")
        _uiState.update { it.copy(isRefreshing = true) }
        locationJob?.cancel()
        locationJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                locationClient.getCurrentLocation().fold(
                    onSuccess = { loc ->
                        logger.i("Location fetched for refresh - lat: ${loc.latitude}, lon: ${loc.longitude}")
                        preferenceManager.saveLocationPreferences(loc.latitude to loc.longitude)
                        performInitialDataLoading(forceRefresh = true)
                    },
                    onFailure = { e ->
                        logger.e("Location fetch failed during refresh", e)
                        val isGpsDisabled = e is LocationClient.LocationException &&
                                e.message?.contains("GPS is disabled", ignoreCase = true) == true
                        val error = if (isGpsDisabled)
                            UiText.StringResource(resId = R.string.gps_disabled_error_txt)
                        else if (e is Exception) errorResponseFromException(e)
                        else UiText.StringResource(resId = R.string.general_error_txt)
                        _uiState.update {
                            it.copy(isRefreshing = false, error = error, isGpsDisabled = isGpsDisabled)
                        }
                    }
                )
            } catch (_: CancellationException) {
                logger.d("Pull-to-refresh cancelled")
            } catch (e: Exception) {
                logger.e("Error during pull-to-refresh", e)
                _uiState.update {
                    it.copy(isRefreshing = false, error = errorResponseFromException(e))
                }
            }
        }
    }

    /** Load weather and air quality data for UI. */
    private fun performInitialDataLoading(forceRefresh: Boolean = false) {
        logger.d("Starting initial data loading (forceRefresh=$forceRefresh)")
        dataLoadingJob?.cancel()
        dataLoadingJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                val prefs = preferenceManager.getUserPreferencesFlow().first()
                val lat = prefs.latitude
                val lon = prefs.longitude

                if (lat != null && lon != null) {
                    val location = lat to lon
                    logger.d("Loading data for location - lat: $lat, lon: $lon")

                    refreshWeatherReport(location, forceRefresh)
                    logger.v("Refreshed weather report cache")

                    getAirQuality(location.first, location.second)
                        .combine(getWeatherReport(location)) { air, weather ->
                            logger.d("Data loaded successfully")
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
                            logger.e("Error loading weather data", e)
                            val error = if (e is Exception) errorResponseFromException(e)
                            else UiText.StringResource(resId = R.string.general_error_txt)
                            _uiState.update {
                                it.copy(isLoading = false, isRefreshing = false, error = error)
                            }
                        }
                        .collectLatest { state -> _uiState.value = state }
                } else {
                    logger.w("Location coordinates not found in preferences")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = UiText.StringResource(R.string.default_coordinates_txt)
                        )
                    }
                }
            } catch (_: CancellationException) {
                logger.d("Initial data loading cancelled")
            } catch (e: Exception) {
                logger.e("Error in initial data loading", e)
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = errorResponseFromException(e))
                }
            }
        }
    }

    /** Login with email and password. */
    fun login(email: String, password: String) = launchAuth("Login", email) {
        authRepository.login(email, password)
    }

    /** Register with email and password. */
    fun register(email: String, password: String) = launchAuth("Registration", email) {
        authRepository.register(
            email = email,
            password = password,
            timestampOfRegistration = deviceInfoProvider.getCurrentUtcTimestamp(),
            deviceModel = deviceInfoProvider.getDeviceModel(),
            operatingSystem = deviceInfoProvider.getOperatingSystem(),
            osVersion = deviceInfoProvider.getOsVersion(),
            appVersion = deviceInfoProvider.getAppVersion(),
            ipAddress = deviceInfoProvider.getIpAddress(),
            registrationSource = deviceInfoProvider.getRegistrationSource(),
            firebaseToken = deviceInfoProvider.getFirebaseToken()
        )
    }

    private fun launchAuth(
        actionName: String,
        email: String,
        block: suspend () -> AuthResponse
    ) = viewModelScope.launch(dispatchers.io) {
        logger.d("$actionName attempt for email: $email")
        _authState.value = AuthState.Loading
        try {
            handleAuthResponse(block())
        } catch (e: Exception) {
            logger.e("$actionName failed for email: $email", e)
            _authState.value = AuthState.Error(UiText.DynamicText(e.message ?: "$actionName failed"))
        }
    }

    /** Logout user. */
    fun logout() = viewModelScope.launch(dispatchers.io) {
        logger.d("Logout initiated")
        _authState.value = AuthState.LogoutLoading
        authRepository.logout().fold(
            onSuccess = {
                logger.i("Logout successful")
                _authState.value = AuthState.LoggedOut
            },
            onFailure = { e ->
                logger.e("Logout failed", e)
                _authState.value = AuthState.Error(UiText.DynamicText(e.message ?: "Logout failed"))
            }
        )
    }

    private fun silentTokenRefresh() = viewModelScope.launch(dispatchers.io) {
        logger.d("Starting silent token refresh")
        when (val result = tokenManager.refreshToken()) {
            is TokenResult.Valid -> logger.i("Token refreshed successfully")
            is TokenResult.NoToken -> {
                tokenManager.forceLogout()
                emit(AuthEvent.Unauthorized("Your session has expired. Please log in again."))
            }
            is TokenResult.InvalidToken -> {
                tokenManager.forceLogout()
                emit(AuthEvent.Unauthorized("Your session has expired. Please log in again."))
            }
            is TokenResult.Error -> {
                logger.e("Silent token refresh error", result.exception)
                emit(AuthEvent.Unauthorized("Your session has expired. Please log in again."))
            }
        }
    }

    private fun handleAuthResponse(response: AuthResponse) {
        val data = response.data
            ?.takeIf { response.isSuccess() && it.token.isNotBlank() }
            ?: run {
                logger.w("Authentication failed - success: ${response.isSuccess()}")
                _authState.value =
                    AuthState.Error(UiText.DynamicText(response.message ?: "Authentication failed"))
                return
            }

        logger.i("Authentication successful")

        if (!data.isPremium) {
            _authState.value = AuthState.Success
            return
        }

        // Save premium status to preferences so PaymentViewModel observes the update reactively.
        viewModelScope.launch(dispatchers.io) {
            logger.i("User is premium, saving premium status")
            val expiryMillis = data.premiumExpiresAt?.let { parseIsoToMillis(it) }
                ?: (Clock.System.now().toEpochMilliseconds() + 365L * 24 * 60 * 60 * 1000)
            preferenceManager.savePremiumStatus(isPremium = true, expiryMillis = expiryMillis)
            withContext(dispatchers.main) {
                _authState.value = AuthState.Success
            }
        }
    }

    private fun parseIsoToMillis(isoDate: String): Long? = try {
        Instant.parse(isoDate).toEpochMilliseconds()
    } catch (_: Exception) {
        null
    }

    /** Reset authentication state. */
    fun resetAuthState() {
        logger.d("Auth state reset to Initial")
        _authState.value = AuthState.Initial
    }

    override fun onCleared() {
        super.onCleared()
        logger.d("MainViewModel cleared - cancelling all jobs")
        notificationBannerJob?.cancel()
        locationJob?.cancel()
        dataLoadingJob?.cancel()
    }
}

/** Authentication state. */
sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object LogoutLoading : AuthState()
    object Success : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: UiText) : AuthState()
}
