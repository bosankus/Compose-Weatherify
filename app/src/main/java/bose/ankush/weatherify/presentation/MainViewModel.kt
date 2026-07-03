package bose.ankush.weatherify.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.commonui.locations.PlaceSearchUiState
import bose.ankush.commonui.locations.SavedLocationsUiState
import bose.ankush.network.auth.utils.isPremiumActive
import bose.ankush.weatherify.R
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
import bose.ankush.weatherify.domain.use_case.GetAirQuality
import bose.ankush.weatherify.domain.use_case.GetWeatherReport
import bose.ankush.weatherify.domain.use_case.RefreshWeatherReport
import bose.ankush.weatherify.domain.use_case.SavedLocationsUseCase
import bose.ankush.weatherify.domain.use_case.SearchPlacesUseCase
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
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Main ViewModel for Weatherify.
 * Handles weather UI state, location, and notifications.
 *
 * Auth state and logic lives in [bose.ankush.auth.presentation.AuthViewModel].
 * Payment state and logic lives in [bose.ankush.payment.presentation.PaymentViewModel].
 */
@OptIn(FlowPreview::class)
@Suppress("TooGenericExceptionCaught")
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        loggerFactory: LoggerFactory,
        private val refreshWeatherReport: RefreshWeatherReport,
        private val getWeatherReport: GetWeatherReport,
        private val getAirQuality: GetAirQuality,
        private val weatherRepository: WeatherRepository,
        private val locationClient: LocationClient,
        private val preferenceManager: PreferenceManager,
        private val dispatchers: DispatcherProvider,
        private val remoteConfigService: RemoteConfigService,
        private val searchPlacesUseCase: SearchPlacesUseCase,
        private val savedLocationsUseCase: SavedLocationsUseCase,
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

            // Reactively refresh weather data when premium tier changes (activation or expiry).
            // drop(1) skips the initial emission so we only react to actual changes.
            viewModelScope.launch(dispatchers.io) {
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
                    .debounce(500.milliseconds)
                    .filter { it.length >= MIN_QUERY_LENGTH }
                    .distinctUntilChanged()
                    .collectLatest { query -> fetchPlaceSuggestions(query) }
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
                                            ignoreCase = true,
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
                                            ignoreCase = true,
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
                                        isGpsDisabled = isGpsDisabled,
                                    )
                                }
                            },
                        )
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
                        val isOverridden =
                            prefs.isLocationOverridden &&
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
                                error = errorResponseFromException(e),
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
                        if (e is Exception) {
                            errorResponseFromException(e)
                        } else {
                            UiText.StringResource(resId = R.string.general_error_txt)
                        }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error,
                        )
                    }
                }.collectLatest { state -> _uiState.value = state }
        }

        /** Called by MainActivity after AuthEffect.LoggedOut — clears local weather data and prefs. */
        fun handleLoggedOut() {
            viewModelScope.launch(dispatchers.io) {
                logger.i("Clearing local data after logout")
                weatherRepository.clearAllData()
                preferenceManager.clearAll()
            }
        }

        /** Called by MainActivity after AuthEffect.PremiumStatusChanged — persists premium state. */
        fun updatePremiumStatus(
            isPremium: Boolean,
            expiryMillis: Long?,
        ) {
            viewModelScope.launch(dispatchers.io) {
                logger.d("Saving premium status: isPremium=$isPremium, expiryMillis=$expiryMillis")
                preferenceManager.savePremiumStatus(isPremium = isPremium, expiryMillis = expiryMillis)
            }
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
        fun setDefaultLocation(
            lat: Double,
            lon: Double,
            name: String,
        ) {
            _uiState.update { UIState(isLoading = true) }
            dataLoadingJob?.cancel()
            dataLoadingJob =
                viewModelScope.launch(dataFetchExceptionHandler + dispatchers.io) {
                    try {
                        logger.i("Setting default location override: $name ($lat, $lon)")
                        preferenceManager.saveLocationOverride(lat, lon, name)
                        // Use coordinates directly — avoids DataStore re-read race condition
                        fetchWeatherData(
                            lat,
                            lon,
                            isOverridden = true,
                            overrideName = name,
                            forceRefresh = true,
                        )
                    } catch (_: CancellationException) {
                        logger.d("setDefaultLocation cancelled")
                    } catch (e: Exception) {
                        logger.e("Error setting default location", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = errorResponseFromException(e),
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
            logger.d("MainViewModel cleared - cancelling all jobs")
            notificationBannerJob?.cancel()
            locationJob?.cancel()
            dataLoadingJob?.cancel()
            super.onCleared()
        }
    }

private const val MIN_QUERY_LENGTH = 2
