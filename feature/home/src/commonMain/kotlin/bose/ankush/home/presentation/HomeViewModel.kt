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
import bose.ankush.home.generated.resources.location_permission_denied_txt
import bose.ankush.home.presentation.util.CoordinateResolution
import bose.ankush.home.presentation.util.errorMessageFromException
import bose.ankush.storage.api.LocationPreferencesStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
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

    private val refreshTrigger =
        MutableSharedFlow<Boolean>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

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
        // Initialize Firebase Remote Config
        remoteConfigGate.initialize()

        val overrideChanged =
            locationPreferencesStorage
                .getLocationPreferencesFlow()
                .map { it.isLocationOverridden to (it.overrideLat to it.overrideLon) }
                .distinctUntilChanged()
                .drop(1)
                .map { true }

        viewModelScope.launch(dataFetchExceptionHandler) {
            merge(refreshTrigger, overrideChanged)
                .onStart { emit(false) }
                .catch {
                    if (it !is CancellationException) {
                        val message =
                            if (it is Exception) {
                                errorMessageFromException(it)
                            } else {
                                getString(Res.string.general_error_txt)
                            }
                        dispatch(HomeAction.Error(message))
                    }
                }.collectLatest { forceRefresh -> runFetch(forceRefresh) }
        }
    }

    private fun dispatch(action: HomeAction) {
        _state.update { HomeReducer.reduce(it, action) }
    }

    internal fun processIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.FetchLocation, HomeIntent.Refresh -> refreshTrigger.tryEmit(value = true)
            HomeIntent.ResetLocationOverride -> resetLocationOverride()
            HomeIntent.RequestLocationPermission ->
                _effect.trySend(element = HomeEffect.RequestLocationPermission)
            HomeIntent.EnableNotificationBanner ->
                _effect.trySend(
                    element =
                        if (_state.value.isNotificationPermissionPermanentlyDeclined) {
                            HomeEffect.OpenSettings
                        } else {
                            HomeEffect.RequestNotificationPermission
                        },
                )

            HomeIntent.DismissNotificationBanner ->
                dispatch(action = HomeAction.DismissNotificationBanner)

            is HomeIntent.UpdateNotificationPermissionState ->
                updateNotificationBannerVisibility(hasPermission = intent.hasPermission)

            is HomeIntent.NotificationPermissionResult -> handlePermissionResult(intent = intent)
        }
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
                resetDismissal = true,
            ),
        )
    }

    private suspend fun runFetch(forceRefresh: Boolean) {
        dispatch(HomeAction.Loading(isRefreshing = forceRefresh))
        when (val resolution = resolveCoordinates()) {
            is CoordinateResolution.Ready ->
                fetchWeatherData(
                    lat = resolution.lat,
                    lon = resolution.lon,
                    isOverridden = resolution.isOverridden,
                    overrideName = resolution.overrideName,
                    forceRefresh = forceRefresh,
                )

            is CoordinateResolution.LocationError -> {
                dispatch(
                    HomeAction.SetOffline(
                        message = resolution.message,
                        isOffline = true,
                        isGpsDisabled = resolution.isGpsDisabled,
                        isLocationPermissionDenied = resolution.isPermissionDenied,
                    ),
                )
                resolution.fallback?.let {
                    fetchWeatherData(it.lat, it.lon, it.isOverridden, it.overrideName, forceRefresh)
                }
            }

            CoordinateResolution.NoCoordinates ->
                dispatch(
                    HomeAction.Error(
                        getString(Res.string.default_coordinates_txt),
                    ),
                )
        }
    }

    private suspend fun resolveCoordinates(): CoordinateResolution {
        val prefs = locationPreferencesStorage.getLocationPreferencesFlow().first()

        if (prefs.isLocationOverridden) {
            val hasOverride = prefs.overrideLat != null && prefs.overrideLon != null
            val lat = prefs.overrideLat ?: prefs.latitude
            val lon = prefs.overrideLon ?: prefs.longitude
            return if (lat != null && lon != null) {
                CoordinateResolution.Ready(
                    lat = lat,
                    lon = lon,
                    isOverridden = hasOverride,
                    overrideName = if (hasOverride) prefs.overrideLocationName else null,
                )
            } else {
                CoordinateResolution.NoCoordinates
            }
        }

        if (!locationClient.hasLocationPermission()) {
            val fallback =
                prefs.latitude?.let { lat ->
                    prefs.longitude?.let { lon ->
                        CoordinateResolution.Ready(lat, lon, isOverridden = false, overrideName = null)
                    }
                }
            return CoordinateResolution.LocationError(
                message = getString(Res.string.location_permission_denied_txt),
                isGpsDisabled = false,
                isPermissionDenied = true,
                fallback = fallback,
            )
        }

        return locationClient.getCurrentLocation().fold(
            onSuccess = { loc ->
                locationPreferencesStorage.saveLocationPreferences(loc.latitude to loc.longitude)
                CoordinateResolution.Ready(
                    loc.latitude,
                    loc.longitude,
                    isOverridden = false,
                    overrideName = null,
                )
            },
            onFailure = { e ->
                val isGpsDisabled = e is LocationClient.LocationException
                val message =
                    (e as? Exception)?.let { errorMessageFromException(it) }
                        ?: getString(Res.string.general_error_txt)
                val fallback =
                    prefs.latitude?.let { lat ->
                        prefs.longitude?.let { lon ->
                            CoordinateResolution.Ready(
                                lat,
                                lon,
                                isOverridden = false,
                                overrideName = null,
                            )
                        }
                    }
                CoordinateResolution.LocationError(message, isGpsDisabled, fallback = fallback)
            },
        )
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
        }
    }
}
