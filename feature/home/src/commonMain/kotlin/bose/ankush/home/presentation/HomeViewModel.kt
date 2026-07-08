package bose.ankush.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.usecase.GetAirQuality
import bose.ankush.home.domain.usecase.GetWeatherReport
import bose.ankush.home.domain.usecase.RefreshWeatherReport
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.default_coordinates_txt
import bose.ankush.home.generated.resources.general_error_txt
import bose.ankush.home.generated.resources.gps_disabled_error_txt
import bose.ankush.home.presentation.util.errorMessageFromException
import bose.ankush.storage.api.LocationPreferencesStorage
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
 * Home's MVI ViewModel. Reactively observes [LocationPreferencesStorage] for location-override
 * changes so a location pinned from a sibling tab (via [bose.ankush.home.HomeLocationCoordinator])
 * is picked up without a direct cross-module ViewModel reference.
 */
internal class HomeViewModel(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val locationClient: LocationClient,
    private val locationPreferencesStorage: LocationPreferencesStorage,
    private val remoteConfigGate: HomeRemoteConfigGate,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    // performInitialDataLoading/runLocationFetch always run inside this job's coroutine,
    // so a single handle is enough to cancel an in-flight location+weather fetch.
    private var dataFetchJob: Job? = null

    private val dataFetchExceptionHandler =
        CoroutineExceptionHandler { _, e ->
            if (e !is CancellationException) {
                viewModelScope.launch {
                    val message =
                        if (e is Exception) errorMessageFromException(e) else getString(Res.string.general_error_txt)
                    dispatch(HomeAction.Error(message))
                }
            }
        }

    init {
        remoteConfigGate.initialize()

        // drop(1) skips the initial emission — only react to an actual override change made
        // after this ViewModel started observing (e.g. a saved location pinned elsewhere).
        viewModelScope.launch {
            locationPreferencesStorage
                .getLocationPreferencesFlow()
                .map { it.isLocationOverridden to (it.overrideLat to it.overrideLon) }
                .distinctUntilChanged()
                .drop(1)
                .collect { fetchAndSaveLocationCoordinates(forceRefresh = true) }
        }

        fetchAndSaveLocationCoordinates(false)
    }

    fun processIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.FetchLocation, HomeIntent.Refresh ->
                fetchAndSaveLocationCoordinates(forceRefresh = true)

            HomeIntent.ResetLocationOverride -> resetLocationOverride()
            HomeIntent.EnableNotificationBanner -> _effect.trySend(
                if (_state.value.isNotificationPermissionPermanentlyDeclined) {
                    HomeEffect.OpenSettings
                } else {
                    HomeEffect.RequestNotificationPermission
                },
            )

            HomeIntent.DismissNotificationBanner ->
                dispatch(HomeAction.DismissNotificationBanner)

            is HomeIntent.UpdateNotificationPermissionState ->
                updateNotificationBannerVisibility(hasPermission = intent.hasPermission)

            is HomeIntent.NotificationPermissionResult -> handlePermissionResult(intent)
        }
    }

    private fun dispatch(action: HomeAction) {
        _state.update { HomeReducer.reduce(it, action) }
    }

    private fun updateNotificationBannerVisibility(hasPermission: Boolean) {
        viewModelScope.launch(dataFetchExceptionHandler) {
            val enabled = remoteConfigGate.isNotificationBannerEnabled()
            dispatch(HomeAction.UpdateNotificationBanner(show = enabled && !hasPermission))
        }
    }

    private fun handlePermissionResult(intent: HomeIntent.NotificationPermissionResult) {
        dispatch(
            HomeAction.UpdateNotificationBanner(
                show = !intent.isGranted,
                isPermanentlyDeclined = intent.isPermanentlyDeclined,
                resetDismissal = true
            ),
        )
    }

    private fun fetchAndSaveLocationCoordinates(forceRefresh: Boolean) {
        dispatch(HomeAction.Loading(isRefreshing = forceRefresh))
        dataFetchJob?.cancel()
        dataFetchJob =
            viewModelScope.launch(dataFetchExceptionHandler) {
                val prefs = locationPreferencesStorage.getLocationPreferencesFlow().first()
                if (prefs.isLocationOverridden) {
                    performInitialDataLoading(forceRefresh = forceRefresh)
                } else {
                    runLocationFetch(forceRefresh = forceRefresh)
                }
            }
    }

    private suspend fun runLocationFetch(forceRefresh: Boolean) {
        locationClient.getCurrentLocation().fold(
            onSuccess = { loc ->
                locationPreferencesStorage.saveLocationPreferences(loc.latitude to loc.longitude)
                performInitialDataLoading(forceRefresh = forceRefresh)
            },
            onFailure = { e -> handleLocationFailure(e, forceRefresh) },
        )
    }

    // A fresh GPS fix can fail transiently (cold start, indoors, brief permission race). Rather
    // than blocking the whole screen, fall back to the last known (non-override) coordinates so
    // weather still loads, and surface the failure as a dismissible banner instead.
    private suspend fun handleLocationFailure(
        e: Throwable,
        forceRefresh: Boolean,
    ) {
        val isGpsDisabled =
            e is LocationClient.LocationException &&
                    e.message?.contains("GPS is disabled", ignoreCase = true) == true
        val errorMessage =
            when {
                isGpsDisabled -> getString(Res.string.gps_disabled_error_txt)
                e is Exception -> errorMessageFromException(e)
                else -> getString(Res.string.general_error_txt)
            }

        dispatch(
            HomeAction.SetOffline(
                message = errorMessage,
                isOffline = true,
                isGpsDisabled = isGpsDisabled,
            ),
        )

        val prefs = locationPreferencesStorage.getLocationPreferencesFlow().first()
        val lastKnownLat = prefs.latitude
        val lastKnownLon = prefs.longitude
        if (!prefs.isLocationOverridden && lastKnownLat != null && lastKnownLon != null) {
            fetchWeatherData(
                lat = lastKnownLat,
                lon = lastKnownLon,
                isOverridden = false,
                overrideName = null,
                forceRefresh = forceRefresh,
            )
        }
    }

    private suspend fun performInitialDataLoading(forceRefresh: Boolean) {
        val prefs = locationPreferencesStorage.getLocationPreferencesFlow().first()
        val isOverridden =
            prefs.isLocationOverridden && prefs.overrideLat != null &&
                    prefs.overrideLon != null
        val lat = if (isOverridden) prefs.overrideLat else prefs.latitude
        val lon = if (isOverridden) prefs.overrideLon else prefs.longitude
        val overrideName = if (isOverridden) prefs.overrideLocationName else null

        if (lat != null && lon != null) {
            fetchWeatherData(lat, lon, isOverridden, overrideName, forceRefresh)
        } else {
            dispatch(
                HomeAction.Error(
                    getString(Res.string.default_coordinates_txt),
                ),
            )
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
                dispatch(HomeAction.Error(message = error))
            }.collectLatest { newState ->
                dispatch(
                    HomeAction.Success(
                        location = location,
                        isLocationOverridden = isOverridden,
                        overrideLocationName = overrideName,
                        weather = newState.weatherData,
                        airQuality = newState.airQualityData,
                    ),
                )
            }
    }

    private fun resetLocationOverride() {
        viewModelScope.launch(dataFetchExceptionHandler) {
            locationPreferencesStorage.clearLocationOverride()
            fetchAndSaveLocationCoordinates(forceRefresh = true)
        }
    }

    override fun onCleared() {
        dataFetchJob?.cancel()
        super.onCleared()
    }
}
