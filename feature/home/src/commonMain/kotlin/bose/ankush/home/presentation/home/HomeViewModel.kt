package bose.ankush.home.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.home.data.preferences.HomeWeatherPreferences
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.usecase.GetAirQuality
import bose.ankush.home.domain.usecase.GetWeatherReport
import bose.ankush.home.domain.usecase.RefreshWeatherReport
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.default_coordinates_txt
import bose.ankush.home.generated.resources.general_error_txt
import bose.ankush.home.generated.resources.gps_disabled_error_txt
import bose.ankush.home.presentation.home.util.errorMessageFromException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Home's MVI ViewModel. Reactively observes [HomeWeatherPreferences] for location-override
 * changes so a location pinned from a sibling tab (via [bose.ankush.home.HomeLocationCoordinator])
 * is picked up without a direct cross-module ViewModel reference.
 */
internal class HomeViewModel(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val locationClient: LocationClient,
    private val preferences: HomeWeatherPreferences,
    private val remoteConfigGate: HomeRemoteConfigGate,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    private var locationJob: Job? = null
    private var dataLoadingJob: Job? = null

    private val dataFetchExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            if (e !is CancellationException) {
                viewModelScope.launch {
                    val message =
                        if (e is Exception) errorMessageFromException(e) else getString(Res.string.general_error_txt)
                    _state.update { HomeState(error = message) }
                }
            }
        }

    init {
        remoteConfigGate.initialize()

        // drop(1) skips the initial emission — only react to an actual override change made
        // after this ViewModel started observing (e.g. a saved location pinned elsewhere).
        viewModelScope.launch {
            preferences
                .getUserPreferencesFlow()
                .map { it.isLocationOverridden to (it.overrideLat to it.overrideLon) }
                .distinctUntilChanged()
                .drop(1)
                .collect { fetchAndSaveLocationCoordinates(forceRefresh = true) }
        }

        fetchAndSaveLocationCoordinates()
    }

    fun processIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.FetchLocation -> fetchAndSaveLocationCoordinates()
            HomeIntent.Refresh -> refreshWeatherData()
            HomeIntent.ResetLocationOverride -> resetLocationOverride()
            HomeIntent.EnableNotificationBanner -> _effect.trySend(HomeEffect.RequestNotificationPermission)
            HomeIntent.DismissNotificationBanner -> _state.update { it.copy(showNotificationBanner = false) }
            is HomeIntent.UpdateNotificationPermissionState ->
                updateNotificationBannerVisibility(
                    intent.hasPermission,
                )

            is HomeIntent.NotificationPermissionResult -> handlePermissionResult(intent)
        }
    }

    private fun updateNotificationBannerVisibility(hasPermission: Boolean) {
        viewModelScope.launch(dataFetchExceptionHandler) {
            val enabled = remoteConfigGate.isNotificationBannerEnabled()
            _state.update { it.copy(showNotificationBanner = enabled && !hasPermission) }
        }
    }

    private fun handlePermissionResult(intent: HomeIntent.NotificationPermissionResult) {
        _state.update {
            it.copy(
                showNotificationBanner = !intent.isGranted,
                isNotificationPermissionPermanentlyDeclined = intent.isPermanentlyDeclined,
            )
        }
    }

    private fun fetchAndSaveLocationCoordinates(forceRefresh: Boolean = false) {
        _state.update { it.copy(isLoading = true) }
        locationJob?.cancel()
        locationJob =
            viewModelScope.launch(dataFetchExceptionHandler) {
                val prefs = preferences.getUserPreferencesFlow().first()
                if (prefs.isLocationOverridden) {
                    performInitialDataLoading(forceRefresh = forceRefresh)
                    return@launch
                }
                runLocationFetch(isRefresh = false, forceRefresh = forceRefresh)
            }
    }

    private fun refreshWeatherData() {
        _state.update { it.copy(isRefreshing = true) }
        locationJob?.cancel()
        locationJob =
            viewModelScope.launch(dataFetchExceptionHandler) {
                val prefs = preferences.getUserPreferencesFlow().first()
                if (prefs.isLocationOverridden) {
                    performInitialDataLoading(forceRefresh = true)
                    return@launch
                }
                runLocationFetch(isRefresh = true, forceRefresh = true)
            }
    }

    private suspend fun runLocationFetch(
        isRefresh: Boolean,
        forceRefresh: Boolean,
    ) {
        try {
            locationClient.getCurrentLocation().fold(
                onSuccess = { loc ->
                    preferences.saveLocationPreferences(loc.latitude to loc.longitude)
                    performInitialDataLoading(forceRefresh = forceRefresh)
                },
                onFailure = { e ->
                    val isGpsDisabled =
                        e is LocationClient.LocationException &&
                            e.message?.contains("GPS is disabled", ignoreCase = true) == true
                    val error =
                        when {
                            isGpsDisabled -> getString(Res.string.gps_disabled_error_txt)
                            e is Exception -> errorMessageFromException(e)
                            else -> getString(Res.string.general_error_txt)
                        }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error,
                            isGpsDisabled = isGpsDisabled,
                        )
                    }
                },
            )
        } catch (_: CancellationException) {
            // superseded by a newer fetch — nothing to surface
        }
    }

    private fun performInitialDataLoading(forceRefresh: Boolean = false) {
        dataLoadingJob?.cancel()
        dataLoadingJob =
            viewModelScope.launch(dataFetchExceptionHandler) {
                val prefs = preferences.getUserPreferencesFlow().first()
                val isOverridden =
                    prefs.isLocationOverridden && prefs.overrideLat != null &&
                        prefs.overrideLon != null
                val lat = if (isOverridden) prefs.overrideLat else prefs.latitude
                val lon = if (isOverridden) prefs.overrideLon else prefs.longitude
                val overrideName = if (isOverridden) prefs.overrideLocationName else null

                if (lat != null && lon != null) {
                    fetchWeatherData(lat, lon, isOverridden, overrideName, forceRefresh)
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = getString(Res.string.default_coordinates_txt),
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
        refreshWeatherReport(location, forceRefresh)

        getAirQuality(location.first, location.second)
            .combine(getWeatherReport(location)) { air, weather ->
                HomeState(
                    isLoading = false,
                    userLocation = location,
                    weatherData = weather,
                    airQualityData = air,
                    error = null,
                    isLocationOverridden = isOverridden,
                    activeLocationName = overrideName,
                )
            }.catch { e ->
                if (e is CancellationException) throw e
                val error =
                    if (e is Exception) {
                        errorMessageFromException(e)
                    } else {
                        getString(Res.string.general_error_txt)
                    }
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = error) }
            }.collectLatest { newState ->
                _state.update { current ->
                    newState.copy(
                        showNotificationBanner = current.showNotificationBanner,
                        isNotificationPermissionPermanentlyDeclined =
                            current.isNotificationPermissionPermanentlyDeclined,
                        isOffline = current.isOffline,
                    )
                }
            }
    }

    private fun resetLocationOverride() {
        viewModelScope.launch(dataFetchExceptionHandler) {
            preferences.clearLocationOverride()
            fetchAndSaveLocationCoordinates()
        }
    }

    override fun onCleared() {
        locationJob?.cancel()
        dataLoadingJob?.cancel()
        super.onCleared()
    }
}
