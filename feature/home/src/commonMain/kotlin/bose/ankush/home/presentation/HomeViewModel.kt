package bose.ankush.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.finder.domain.usecase.GetSavedLocationsUseCase
import bose.ankush.home.domain.leaveby.LeaveByFakeDoorEligibility
import bose.ankush.home.domain.leaveby.LeaveByPlace
import bose.ankush.home.domain.location.LocationClient
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.usecase.GetAirQuality
import bose.ankush.home.domain.usecase.GetWeatherReport
import bose.ankush.home.domain.usecase.RefreshWeatherReport
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.default_coordinates_txt
import bose.ankush.home.generated.resources.general_error_txt
import bose.ankush.home.generated.resources.location_permission_denied_txt
import bose.ankush.home.presentation.util.CoordinateResolution
import bose.ankush.home.presentation.util.errorMessageFromException
import bose.ankush.storage.api.LocationPreferencesStorage
import bose.ankush.storage.model.LocationPreferences
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock

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
    private val analyticsTracker: AnalyticsTracker,
    private val getSavedLocationsUseCase: GetSavedLocationsUseCase,
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

    private var leaveByJob: Job? = null

    init {
        // Initialize Firebase Remote Config. Re-check the fake door once activate finishes
        // so a freshly fetched flag is not stuck on the in-app default for this session.
        remoteConfigGate.initialize(onActivated = { refreshLeaveByEligibility() })
        refreshLeaveByEligibility()

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
            HomeIntent.FetchLocation, HomeIntent.Refresh -> {
                refreshTrigger.tryEmit(value = true)
                refreshLeaveByEligibility()
            }
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

            HomeIntent.DismissNotificationBanner -> {
                analyticsTracker.track(AnalyticsEvent.NotificationBannerDismissed)
                dispatch(action = HomeAction.DismissNotificationBanner)
            }

            is HomeIntent.UpdateNotificationPermissionState ->
                updateNotificationBannerVisibility(hasPermission = intent.hasPermission)

            is HomeIntent.NotificationPermissionResult -> handlePermissionResult(intent = intent)

            HomeIntent.RefreshLeaveByEligibility -> refreshLeaveByEligibility()

            HomeIntent.JoinLeaveByList -> joinLeaveByList()

            HomeIntent.DismissLeaveByCard -> dismissLeaveByCard()

            HomeIntent.NoteLeaveByMisleading -> noteLeaveByMisleading()
        }
    }

    /**
     * Joined / dismissed / misleading live in [HomeState] for this ViewModel session only.
     * No preference, no network write, no notification schedule.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun refreshLeaveByEligibility() {
        leaveByJob?.cancel()
        leaveByJob =
            viewModelScope.launch {
                try {
                    if (!remoteConfigGate.isLeaveByFakeDoorEnabled()) {
                        publishLeaveBy(eligible = false)
                        return@launch
                    }
                    val places =
                        getSavedLocationsUseCase()
                            .getOrNull()
                            ?.map { LeaveByPlace(lat = it.lat, lon = it.lon) }
                            .orEmpty()
                    val localNow = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    publishLeaveBy(LeaveByFakeDoorEligibility.isEligible(places, localNow))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    publishLeaveBy(eligible = false)
                }
            }
    }

    private fun publishLeaveBy(eligible: Boolean) {
        val current = _state.value
        if (current.isLeaveByDismissed) {
            if (current.showLeaveByCard) {
                dispatch(HomeAction.UpdateLeaveByCard(show = false))
            }
            return
        }
        if (eligible && !current.showLeaveByCard) {
            analyticsTracker.track(AnalyticsEvent.LeaveByFakeDoorImpression(surface = LEAVE_BY_SURFACE))
        }
        if (current.showLeaveByCard != eligible) {
            dispatch(HomeAction.UpdateLeaveByCard(show = eligible))
        }
    }

    private fun joinLeaveByList() {
        val current = _state.value
        if (!current.showLeaveByCard || current.hasJoinedLeaveByList) return
        analyticsTracker.track(AnalyticsEvent.LeaveByFakeDoorPrimaryTap(surface = LEAVE_BY_SURFACE))
        dispatch(HomeAction.JoinLeaveByList)
    }

    private fun dismissLeaveByCard() {
        if (!_state.value.showLeaveByCard) return
        analyticsTracker.track(AnalyticsEvent.LeaveByFakeDoorDismiss(surface = LEAVE_BY_SURFACE))
        dispatch(HomeAction.DismissLeaveByCard)
    }

    private fun noteLeaveByMisleading() {
        val current = _state.value
        if (!current.showLeaveByCard || current.hasNotedLeaveByMisleading) return
        analyticsTracker.track(AnalyticsEvent.LeaveByFakeDoorMisleadingTap(surface = LEAVE_BY_SURFACE))
        dispatch(HomeAction.NoteLeaveByMisleading)
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
                val isGpsDisabled = (e as? LocationClient.LocationException)?.isGpsDisabled == true
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

    /**
     * Saved coordinates only. This does not ask for a GPS fix, so the first paint is not blocked
     * on location, reverse geocode, or Wear sync.
     */
    private fun savedCoordinates(prefs: LocationPreferences): CoordinateResolution.Ready? {
        if (prefs.isLocationOverridden) {
            val hasOverride = prefs.overrideLat != null && prefs.overrideLon != null
            val lat = prefs.overrideLat ?: prefs.latitude
            val lon = prefs.overrideLon ?: prefs.longitude
            if (lat == null || lon == null) return null
            return CoordinateResolution.Ready(
                lat = lat,
                lon = lon,
                isOverridden = hasOverride,
                overrideName = if (hasOverride) prefs.overrideLocationName else null,
            )
        }
        val lat = prefs.latitude ?: return null
        val lon = prefs.longitude ?: return null
        return CoordinateResolution.Ready(lat, lon, isOverridden = false, overrideName = null)
    }

    /** Emits the Room forecast when one exists. An empty cache leaves the loading state alone. */
    private suspend fun emitSavedForecast() {
        val saved = savedCoordinates(locationPreferencesStorage.getLocationPreferencesFlow().first()) ?: return
        val location = saved.lat to saved.lon
        val cached = getWeatherReport(location).first() ?: return
        val air = getAirQuality(saved.lat, saved.lon).first()
        dispatch(
            HomeAction.Success(
                location = location,
                isLocationOverridden = saved.isOverridden,
                overrideLocationName = saved.overrideName,
                weather = cached,
                airQuality = air,
            ),
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
        coroutineScope {
            val refreshFinished = MutableStateFlow(false)
            launch {
                try {
                    refreshWeatherReport(location, forceRefresh)
                } finally {
                    refreshFinished.value = true
                }
            }
            combine(
                getAirQuality(location.first, location.second),
                getWeatherReport(location),
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
                // Empty Room stays on the loading state until the refresh settles.
                if (weather == null && !finished) return@collectLatest
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
            locationPreferencesStorage.clearLocationOverride()
        }
    }

    private companion object {
        /** Wander home is the only surface that shows this card. */
        const val LEAVE_BY_SURFACE = "wander_home"
    }
}
