package bose.ankush.weatherify.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.commonui.locations.PlaceSearchUiState
import bose.ankush.commonui.locations.SavedLocationsUiState
import bose.ankush.network.auth.events.AuthEvent
import bose.ankush.network.auth.events.AuthEventBus.emit
import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.auth.utils.isPremiumActive
import bose.ankush.network.domain.SavedLocationsUseCase
import bose.ankush.network.domain.SearchPlacesUseCase
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.DeviceInfoProvider
import bose.ankush.weatherify.base.common.ENABLE_NOTIFICATION
import bose.ankush.weatherify.base.common.LoggerFactory
import bose.ankush.weatherify.base.common.UiText
import bose.ankush.weatherify.base.common.errorResponseFromException
import bose.ankush.weatherify.base.dispatcher.DispatcherProvider
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.base.location.LocationPermissions
import bose.ankush.weatherify.domain.preference.PreferenceManager
import bose.ankush.weatherify.domain.remote_config.RemoteConfigService
import bose.ankush.weatherify.domain.repository.WeatherRepository
import bose.ankush.weatherify.domain.use_case.get_air_quality.GetAirQuality
import bose.ankush.weatherify.domain.use_case.get_weather_reports.GetWeatherReport
import bose.ankush.weatherify.domain.use_case.refresh_weather_reports.RefreshWeatherReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
@OptIn(FlowPreview::class)
@Suppress("TooGenericExceptionCaught")
@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val weatherRepository: WeatherRepository,
    private val locationClient: LocationClient,
    private val preferenceManager: PreferenceManager,
    private val dispatchers: DispatcherProvider,
    private val remoteConfigService: RemoteConfigService,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val searchPlacesUseCase: SearchPlacesUseCase,
    private val savedLocationsUseCase: SavedLocationsUseCase,
    loggerFactory: LoggerFactory,
    private val deviceInfoProvider: DeviceInfoProvider,
) : ViewModel() {
    private val logger = loggerFactory.create("${MainViewModel::class.simpleName} ->")

    var permissionDialogQueue = mutableStateListOf<String>()
        private set

    private val _uiState = MutableStateFlow(UIState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _launchNotificationPermission = MutableStateFlow(false)
    val launchNotificationPermission = _launchNotificationPermission.asStateFlow()

    private val _showNotificationCardItem = MutableStateFlow(false)
    val showNotificationCardItem = _showNotificationCardItem.asStateFlow()

    private val _isNotificationPermissionPermanentlyDeclined = MutableStateFlow(false)
    val isNotificationPermissionPermanentlyDeclined =
        _isNotificationPermissionPermanentlyDeclined.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAuthInitialized = MutableStateFlow(false)
    val isAuthInitialized: StateFlow<Boolean> = _isAuthInitialized.asStateFlow()

    private val _savedLocationsState = MutableStateFlow(SavedLocationsUiState())
    val savedLocationsState: StateFlow<SavedLocationsUiState> = _savedLocationsState.asStateFlow()

    private val _placeSearchState = MutableStateFlow(PlaceSearchUiState())
    val placeSearchState: StateFlow<PlaceSearchUiState> = _placeSearchState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    private var notificationBannerJob: Job? = null
    private var locationJob: Job? = null
    private var dataLoadingJob: Job? = null

    private val dataFetchExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            if (e !is CancellationException) {
                val error =
                    if (e is Exception) {
                        errorResponseFromException(e)
                    } else {
                        UiText.StringResource(resId = R.string.general_error_txt)
                    }
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
                }
            }
        }

        // Reactively refresh weather data when premium tier changes (activation or expiry).
        // drop(1) skips the initial emission so we only react to actual changes.
        viewModelScope.launch {
            preferenceManager
                .getUserPreferencesFlow()
                .map { it.isPremium }
                .distinctUntilChanged()
                .drop(1)
                .collect { isPremium ->
                    logger.d("Premium status changed to $isPremium — forcing weather data refresh")
                    performInitialDataLoading(forceRefresh = true)
                }
        }

        viewModelScope.launch(dispatchers.io) {
            _queryFlow
                .debounce(400L)
                .filter { it.length >= MIN_QUERY_LENGTH }
                .distinctUntilChanged()
                .collect { query -> fetchPlaceSuggestions(query) }
        }

        viewModelScope.launch(dispatchers.io) {
            preferenceManager.getUserPreferencesFlow().collect { prefs ->
                val premiumActive =
                    isPremiumActive(
                        prefs.premiumExpiry?.let { millis ->
                            Instant.fromEpochMilliseconds(millis).toString()
                        },
                    )
                val wasPremium = _savedLocationsState.value.isPremium
                _savedLocationsState.update { it.copy(isPremium = premiumActive) }
                if (premiumActive && !wasPremium) loadSavedLocations()
            }
        }
    }

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

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean,
    ) {
        logger.d("Permission result - permission: $permission, granted: $isGranted")
        if (isGranted) {
            logger.d("Permission granted, fetching location")
            fetchAndSaveLocationCoordinates()
        } else if (!permissionDialogQueue.contains(permission)) {
            permissionDialogQueue.add(permission)
            logger.w("Permission denied: $permission, added to queue")
        }
    }

    fun updateNotificationPermission(launchState: Boolean) {
        logger.d("Updating notification permission dialog - show: $launchState")
        _launchNotificationPermission.update { launchState }
    }

    fun updateShowNotificationBannerState(launchState: Boolean) {
        notificationBannerJob?.cancel()
        notificationBannerJob =
            viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
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

    fun updateNotificationPermissionPermanentlyDeclined(isPermanentlyDeclined: Boolean) {
        logger.d("Notification permission permanently declined: $isPermanentlyDeclined")
        _isNotificationPermissionPermanentlyDeclined.update { isPermanentlyDeclined }
    }

    /** Fetch and save user location, then load initial data. Skips GPS when override is active. */
    fun fetchAndSaveLocationCoordinates() {
        logger.d("Starting location fetch")
        _uiState.update { UIState(isLoading = true) }
        locationJob?.cancel()
        locationJob =
            viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
                val prefs = preferenceManager.getUserPreferencesFlow().first()
                if (prefs.isLocationOverridden) {
                    logger.d("Location override active — skipping GPS, using saved location")
                    performInitialDataLoading()
                    return@launch
                }
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
                            val isGpsDisabled =
                                e is LocationClient.LocationException &&
                                        e.message?.contains(
                                            "GPS is disabled",
                                            ignoreCase = true
                                        ) == true
                            val error =
                                if (isGpsDisabled) {
                                    UiText.StringResource(resId = R.string.gps_disabled_error_txt)
                                } else if (e is Exception) {
                                    errorResponseFromException(e)
                                } else {
                                    UiText.StringResource(resId = R.string.general_error_txt)
                                }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = error,
                                    isGpsDisabled = isGpsDisabled,
                                )
                            }
                        },
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

    fun refreshWeatherData() {
        logger.d("Starting pull-to-refresh")
        _uiState.update { it.copy(isRefreshing = true) }
        locationJob?.cancel()
        locationJob =
            viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
                val prefs = preferenceManager.getUserPreferencesFlow().first()
                if (prefs.isLocationOverridden) {
                    logger.d("Location override active — refreshing with saved location")
                    performInitialDataLoading(forceRefresh = true)
                    return@launch
                }
                try {
                    locationClient.getCurrentLocation().fold(
                        onSuccess = { loc ->
                            logger.i("Location fetched for refresh - lat: ${loc.latitude}, lon: ${loc.longitude}")
                            preferenceManager.saveLocationPreferences(loc.latitude to loc.longitude)
                            performInitialDataLoading(forceRefresh = true)
                        },
                        onFailure = { e ->
                            logger.e("Location fetch failed during refresh", e)
                            val isGpsDisabled =
                                e is LocationClient.LocationException &&
                                        e.message?.contains(
                                            "GPS is disabled",
                                            ignoreCase = true
                                        ) == true
                            val error =
                                if (isGpsDisabled) {
                                    UiText.StringResource(resId = R.string.gps_disabled_error_txt)
                                } else if (e is Exception) {
                                    errorResponseFromException(e)
                                } else {
                                    UiText.StringResource(resId = R.string.general_error_txt)
                                }
                            _uiState.update {
                                it.copy(
                                    isRefreshing = false,
                                    error = error,
                                    isGpsDisabled = isGpsDisabled
                                )
                            }
                        },
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

    private fun performInitialDataLoading(forceRefresh: Boolean = false) {
        logger.d("Starting initial data loading (forceRefresh=$forceRefresh)")
        dataLoadingJob?.cancel()
        dataLoadingJob =
            viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
                try {
                    val prefs = preferenceManager.getUserPreferencesFlow().first()
                    val isOverridden = prefs.isLocationOverridden &&
                            prefs.overrideLat != null && prefs.overrideLon != null
                    val lat = if (isOverridden) prefs.overrideLat else prefs.latitude
                    val lon = if (isOverridden) prefs.overrideLon else prefs.longitude
                    val overrideName = if (isOverridden) prefs.overrideLocationName else null

                    if (lat != null && lon != null) {
                        fetchWeatherData(lat, lon, isOverridden, overrideName, forceRefresh)
                    } else {
                        logger.w("Location coordinates not found in preferences")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = UiText.StringResource(R.string.default_coordinates_txt),
                            )
                        }
                    }
                } catch (_: CancellationException) {
                    logger.d("Initial data loading cancelled")
                } catch (e: Exception) {
                    logger.e("Error in initial data loading", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = errorResponseFromException(e)
                        )
                    }
                }
            }
    }

    private suspend fun fetchWeatherData(
        lat: Double,
        lon: Double,
        isOverridden: Boolean,
        overrideName: String?,
        forceRefresh: Boolean,
    ) {
        val location = lat to lon
        logger.d("Loading data for location - lat: $lat, lon: $lon, overridden: $isOverridden")

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
                    error = null,
                    isLocationOverridden = isOverridden,
                    activeLocationName = overrideName,
                )
            }.flowOn(dispatchers.io)
            .catch { e ->
                if (e is CancellationException) throw e
                logger.e("Error loading weather data", e)
                val error =
                    if (e is Exception) errorResponseFromException(e)
                    else UiText.StringResource(resId = R.string.general_error_txt)
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = error) }
            }.collectLatest { state -> _uiState.value = state }
    }

    fun login(
        email: String,
        password: String,
    ) = launchAuth("Login", email) {
        authRepository.login(email, password)
    }

    fun register(
        email: String,
        password: String,
    ) = launchAuth("Registration", email) {
        authRepository.register(
            email = email,
            password = password,
            timestampOfRegistration = deviceInfoProvider.getCurrentUtcTimestamp(),
            deviceModel = deviceInfoProvider.getDeviceModel(),
            operatingSystem = deviceInfoProvider.getOperatingSystem(),
            osVersion = deviceInfoProvider.getOsVersion(),
            appVersion = deviceInfoProvider.getAppVersion(),
            registrationSource = deviceInfoProvider.getRegistrationSource(),
            firebaseToken = deviceInfoProvider.getFirebaseToken(),
        )
    }

    private fun launchAuth(
        actionName: String,
        email: String,
        block: suspend () -> AuthResponse,
    ) = viewModelScope.launch(dispatchers.io) {
        logger.d("$actionName attempt for email: $email")
        _authState.value = AuthState.Loading
        try {
            handleAuthResponse(block())
        } catch (_: CancellationException) {
            throw CancellationException()
        } catch (e: Exception) {
            logger.e("$actionName failed for email: $email", e)
            _authState.value =
                AuthState.Error(UiText.DynamicText(e.message ?: "$actionName failed"))
        }
    }

    fun logout() =
        viewModelScope.launch(dispatchers.io) {
            logger.d("Logout initiated")
            _authState.value = AuthState.LogoutLoading
            authRepository.logout().fold(
                onSuccess = {
                    logger.i("Logout successful")
                    weatherRepository.clearAllData()
                    preferenceManager.clearAll()
                    _authState.value = AuthState.LoggedOut
                },
                onFailure = { e ->
                    logger.e("Logout failed", e)
                    _authState.value =
                        AuthState.Error(UiText.DynamicText(e.message ?: "Logout failed"))
                },
            )
        }

    /** Called on every app foreground to sync token and premium status with the server. */
    fun refreshTokenOnForeground() =
        viewModelScope.launch(dispatchers.io) {
            try {
                val response = authRepository.refreshToken() ?: return@launch
                if (response.isSuccess()) {
                    val expiryMillis = response.data?.premiumExpiresAt?.let { parseIsoToMillis(it) }
                    val active = isPremiumActive(response.data?.premiumExpiresAt)
                    preferenceManager.savePremiumStatus(
                        isPremium = active,
                        expiryMillis = expiryMillis
                    )
                } else {
                    // 400 Bad Request — token is invalid, force logout
                    tokenManager.forceLogout()
                    emit(AuthEvent.Unauthorized("Your session has expired. Please log in again."))
                }
            } catch (_: CancellationException) {
                // ignore
            } catch (e: Exception) {
                logger.e("Foreground token refresh error", e)
            }
        }

    private fun handleAuthResponse(response: AuthResponse) {
        val data =
            response.data
                ?.takeIf { response.isSuccess() && it.token.isNotBlank() }
                ?: run {
                    logger.w("Authentication failed - success: ${response.isSuccess()}")
                    _authState.value =
                        AuthState.Error(
                            UiText.DynamicText(
                                response.message ?: "Authentication failed"
                            )
                        )
                    return
                }

        logger.i("Authentication successful")

        val premiumActive = isPremiumActive(data.premiumExpiresAt)
        val expiryMillis = data.premiumExpiresAt?.let { parseIsoToMillis(it) }

        if (!premiumActive) {
            viewModelScope.launch(dispatchers.io) {
                preferenceManager.savePremiumStatus(isPremium = false, expiryMillis = expiryMillis)
            }
            _authState.value = AuthState.Success
            return
        }

        // Save premium status to preferences so PaymentViewModel observes the update reactively.
        viewModelScope.launch(dispatchers.io) {
            logger.i("User is premium, saving premium status")
            val millis =
                expiryMillis
                    ?: (Clock.System.now().toEpochMilliseconds() + ONE_YEAR_MILLIS)
            preferenceManager.savePremiumStatus(isPremium = true, expiryMillis = millis)
            withContext(dispatchers.main) {
                _authState.value = AuthState.Success
            }
        }
    }

    private fun parseIsoToMillis(isoDate: String): Long? =
        try {
            Instant.parse(isoDate).toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }

    fun resetAuthState() {
        logger.d("Auth state reset to Initial")
        _authState.value = AuthState.Initial
    }

    fun loadSavedLocations() {
        viewModelScope.launch(dispatchers.io) {
            _savedLocationsState.update { it.copy(isLoading = true, error = null) }
            savedLocationsUseCase.getSavedLocations().fold(
                onSuccess = { locations ->
                    _savedLocationsState.update {
                        it.copy(
                            isLoading = false,
                            locations = locations,
                        )
                    }
                },
                onFailure = { e ->
                    if (e !is CancellationException) {
                        _savedLocationsState.update {
                            it.copy(
                                isLoading = false,
                                error = "Unable to load saved locations. Please try again later.",
                            )
                        }
                    }
                },
            )
        }
    }

    fun saveLocation(
        name: String,
        lat: Double,
        lon: Double,
    ) {
        viewModelScope.launch(dispatchers.io) {
            _savedLocationsState.update { it.copy(isLoading = true, error = null) }
            savedLocationsUseCase.saveLocation(name, lat, lon).fold(
                onSuccess = {
                    _savedLocationsState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Location saved successfully",
                        )
                    }
                    loadSavedLocations()
                },
                onFailure = { e ->
                    if (e !is CancellationException) {
                        _savedLocationsState.update {
                            it.copy(
                                isLoading = false,
                                error = "Unable to save location. Please try again later.",
                            )
                        }
                    }
                },
            )
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch(dispatchers.io) {
            _savedLocationsState.update { it.copy(isLoading = true, error = null) }
            savedLocationsUseCase.deleteLocation(id).fold(
                onSuccess = {
                    _savedLocationsState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Location deleted successfully",
                        )
                    }
                    loadSavedLocations()
                },
                onFailure = { e ->
                    if (e !is CancellationException) {
                        _savedLocationsState.update {
                            it.copy(
                                isLoading = false,
                                error = "Unable to delete location. Please try again later.",
                            )
                        }
                    }
                },
            )
        }
    }

    fun onPlaceSearchQueryChanged(query: String) {
        _placeSearchState.update { it.copy(searchQuery = query, error = null) }
        _queryFlow.value = query
        if (query.length < 2) {
            _placeSearchState.update { it.copy(results = emptyList(), isLoading = false) }
        }
    }

    fun clearPlaceSearch() {
        _placeSearchState.value = PlaceSearchUiState()
        _queryFlow.value = ""
    }

    fun clearLocationMessage() {
        _savedLocationsState.update { it.copy(error = null, successMessage = null) }
    }

    /** Pin a saved location as the default weather source and reload weather data. */
    fun setDefaultLocation(lat: Double, lon: Double, name: String) {
        _uiState.update { UIState(isLoading = true) }
        dataLoadingJob?.cancel()
        dataLoadingJob = viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
            try {
                logger.i("Setting default location override: $name ($lat, $lon)")
                preferenceManager.saveLocationOverride(lat, lon, name)
                // Use coordinates directly — avoids DataStore re-read race condition
                fetchWeatherData(
                    lat,
                    lon,
                    isOverridden = true,
                    overrideName = name,
                    forceRefresh = true
                )
            } catch (_: CancellationException) {
                logger.d("setDefaultLocation cancelled")
            } catch (e: Exception) {
                logger.e("Error setting default location", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorResponseFromException(e)
                    )
                }
            }
        }
    }

    /** Clear the pinned location override and revert to live GPS. */
    fun clearLocationOverride() {
        viewModelScope.launch(dispatchers.io) {
            logger.i("Clearing location override — reverting to GPS")
            preferenceManager.clearLocationOverride()
            withContext(dispatchers.main) {
                fetchAndSaveLocationCoordinates()
            }
        }
    }

    private suspend fun fetchPlaceSuggestions(query: String) {
        _placeSearchState.update { it.copy(isLoading = true, error = null) }
        searchPlacesUseCase(query).fold(
            onSuccess = { suggestions ->
                _placeSearchState.update { it.copy(isLoading = false, results = suggestions) }
            },
            onFailure = { e ->
                if (e !is CancellationException) {
                    _placeSearchState.update {
                        it.copy(
                            isLoading = false,
                            error = "Unable to fetch places. Please try again.",
                        )
                    }
                }
            },
        )
    }

    override fun onCleared() {
        super.onCleared()
        logger.d("MainViewModel cleared - cancelling all jobs")
        notificationBannerJob?.cancel()
        locationJob?.cancel()
        dataLoadingJob?.cancel()
    }
}

private const val ONE_YEAR_MILLIS = 365L * 24 * 60 * 60 * 1000
private const val MIN_QUERY_LENGTH = 2

sealed class AuthState {
    object Initial : AuthState()

    object Loading : AuthState()

    object LogoutLoading : AuthState()

    object Success : AuthState()

    object LoggedOut : AuthState()

    data class Error(
        val message: UiText,
    ) : AuthState()
}
