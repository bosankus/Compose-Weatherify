package bose.ankush.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.home.domain.location.LocationProblem
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.usecase.GetActiveCoordinates
import bose.ankush.home.domain.usecase.GetAirQuality
import bose.ankush.home.domain.usecase.GetWeatherReport
import bose.ankush.home.domain.usecase.RefreshWeatherReport
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.default_coordinates_txt
import bose.ankush.home.generated.resources.general_error_txt
import bose.ankush.home.generated.resources.location_permission_denied_txt
import bose.ankush.home.presentation.util.errorMessageFromException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Home's MVI ViewModel. Refetches when [GetActiveCoordinates.changes] fires, so a location pinned
 * from a sibling tab (via [bose.ankush.home.HomeLocationCoordinator]) is picked up without a
 * direct cross-module ViewModel reference.
 */
internal class HomeViewModel(
    private val refreshWeatherReport: RefreshWeatherReport,
    private val getWeatherReport: GetWeatherReport,
    private val getAirQuality: GetAirQuality,
    private val getActiveCoordinates: GetActiveCoordinates,
    private val remoteConfigGate: HomeRemoteConfigGate,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    /** Background network refresh while a Room forecast is already on screen. Not pull-to-refresh. */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val dataFetchExceptionHandler = CoroutineExceptionHandler { _, e ->
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

        val overrideChanged = getActiveCoordinates.changes().map { true }

        viewModelScope.launch(dataFetchExceptionHandler) {
            merge(refreshTrigger, overrideChanged)
                .onStart { emit(false) }
                .catch {
                    if (it !is CancellationException) dispatch(HomeAction.Error(messageFrom(it)))
                }.collectLatest { forceRefresh ->
                    try {
                        runFetch(forceRefresh)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        dispatch(HomeAction.Error(e.message))
                    }
                }
        }
    }

    private fun dispatch(action: HomeAction) {
        _state.update { HomeReducer.reduce(it, action) }
    }

    internal fun processIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.FetchLocation, HomeIntent.Refresh -> {
                refreshTrigger.tryEmit(value = true)
            }

            HomeIntent.ResetLocationOverride -> resetLocationOverride()
            HomeIntent.RequestLocationPermission ->
                _effect.trySend(element = HomeEffect.RequestLocationPermission)

            HomeIntent.EnableNotificationBanner ->
                _effect.trySend(
                    element =
                        if (_state.value.isNotificationPermissionPermanentlyDeclined) {
                            HomeEffect.OpenNotificationSettings
                        } else {
                            HomeEffect.RequestNotificationPermission
                        },
                )

            HomeIntent.DismissNotificationBanner -> {
                analyticsTracker.track(AnalyticsEvent.NotificationBannerDismissed)
                dispatch(action = HomeAction.DismissNotificationBanner)
            }

            is HomeIntent.UpdateNotificationPermissionState ->
                updateNotificationBannerVisibility(hasPermission = intent.hasPermission)

            is HomeIntent.NotificationPermissionResult -> handlePermissionResult(intent = intent)
        }
    }

    private var notificationBannerVisibilityJob: Job? = null

    private fun updateNotificationBannerVisibility(hasPermission: Boolean) {
        // Cancel any in-flight update first: HomeRoute re-fires this on every permission-flag
        // change (e.g. a stale `false` right before an up-to-date `true` after returning from
        // Settings), and without cancellation the two launches can dispatch out of order, letting
        // the stale one re-show the banner after the fresh one already hid it.
        notificationBannerVisibilityJob?.cancel()
        notificationBannerVisibilityJob =
            viewModelScope.launch(dataFetchExceptionHandler) {
                val enabled = remoteConfigGate.isNotificationBannerEnabled()
                val show = enabled && !hasPermission
                if (show) {
                    analyticsTracker.track(AnalyticsEvent.NotificationBannerShown("remote_config_enabled"))
                }
                dispatch(HomeAction.UpdateNotificationBanner(show = show))
            }
    }

    private fun handlePermissionResult(intent: HomeIntent.NotificationPermissionResult) {
        analyticsTracker.track(
            AnalyticsEvent.PermissionResult(
                permissionType = "notification",
                granted = intent.isGranted,
                permanentlyDeclined = intent.isPermanentlyDeclined,
            ),
        )
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
        // Paint the Room forecast before GPS, reverse geocode, or the network refresh.
        emitSavedForecast()
        val result = getActiveCoordinates.current()
        result.problem?.let { issue ->
            dispatch(
                HomeAction.SetOffline(
                    message = issue.toMessage(),
                    isOffline = true,
                    isGpsDisabled = issue is LocationProblem.GpsDisabled,
                    isLocationPermissionDenied = issue is LocationProblem.PermissionDenied
                ),
            )
        }

        val spot = result.coordinates
        if (spot != null) {
            fetchWeatherData(
                lat = spot.coordinates.latitude,
                lon = spot.coordinates.longitude,
                isOverridden = spot.isOverridden,
                overrideName = spot.overrideName,
                forceRefresh = forceRefresh,
            )
        } else if (result.problem == null) {
            dispatch(
                HomeAction.Error(
                    getString(Res.string.default_coordinates_txt),
                ),
            )
        }
    }

    /**
     * Emits the Room forecast when one exists, before saved coordinates, GPS, or the network.
     * Room keeps one global row, so the read is not gated on coordinates. Saved coordinates
     * are attached afterward when they exist. An empty cache leaves the loading state alone.
     * [HomeAction.CacheChecked] is dispatched either way, so the UI shows the full loading
     * state only once Room is known to be empty.
     */
    private suspend fun emitSavedForecast() {
        if (_state.value.weatherData != null) return
        try {
            val cachedWeather = getWeatherReport.cached().first() ?: return
            val cachedAQ = getAirQuality.cached().first()
            dispatch(HomeAction.Success(weather = cachedWeather, airQuality = cachedAQ))
            dispatch(HomeAction.CacheChecked)
            val savedCoordinates = getActiveCoordinates.lastKnown() ?: return
            dispatch(
                HomeAction.Success(
                    location = savedCoordinates.coordinates.latitude to savedCoordinates.coordinates.longitude,
                    isLocationOverridden = savedCoordinates.isOverridden,
                    overrideLocationName = savedCoordinates.overrideName,
                    weather = _state.value.weatherData ?: cachedWeather,
                    airQuality = _state.value.airQualityData ?: cachedAQ,
                ),
            )
        } finally {
            if (!_state.value.hasCheckedCache) dispatch(HomeAction.CacheChecked)
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
        // A new active location (a saved place picked, or back to GPS) keeps the current
        // forecast and place on screen until the forecast for the new coordinates lands,
        // so Room's single row never pairs the old weather with the new place name.
        // GPS-to-GPS jitter on pull to refresh is not a switch.
        val current = _state.value
        val switchingLocation =
            forceRefresh &&
                    current.userLocation.let { it != null && it != location } &&
                    (isOverridden || current.isLocationOverridden)
        coroutineScope {
            val refreshFinished = MutableStateFlow(false)
            combine(
                getAirQuality(location.first, location.second),
                getWeatherReport.observeWithRefresh(
                    location = location,
                    refresh = { refreshWeatherReport(location, forceRefresh) },
                    onRefreshStart = { _refreshing.value = true },
                    onRefreshEnd = {
                        _refreshing.value = false
                        refreshFinished.value = true
                    },
                ),
                refreshFinished,
            ) { air, weather, finished ->
                Triple(air, weather, finished)
            }.catch { e ->
                if (e is CancellationException) throw e
                val error =
                    if (e is Exception) {
                        errorMessageFromException(e)
                    } else {
                        getString(Res.string.general_error_txt)
                    }
                dispatch(HomeAction.Error(message = error))
            }.collectLatest { (air, weather, finished) ->
                // Empty Room, or a location switch, waits for the refresh to settle.
                if ((weather == null || switchingLocation) && !finished) return@collectLatest
                dispatch(
                    HomeAction.Success(
                        location = location,
                        isLocationOverridden = isOverridden,
                        overrideLocationName = overrideName,
                        weather = weather,
                        airQuality = air,
                    ),
                )
            }
        }
    }

    private fun resetLocationOverride() {
        viewModelScope.launch(dataFetchExceptionHandler) {
            getActiveCoordinates.reset()
        }
    }

    private suspend fun LocationProblem.toMessage(): String =
        when (this) {
            LocationProblem.PermissionDenied -> getString(Res.string.location_permission_denied_txt)
            is LocationProblem.GpsDisabled -> messageFrom(cause)
            is LocationProblem.Failed -> messageFrom(cause)
        }

    private suspend fun messageFrom(cause: Throwable): String =
        (cause as? Exception)?.let { errorMessageFromException(it) }
            ?: getString(Res.string.general_error_txt)
}
