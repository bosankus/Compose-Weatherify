package bose.ankush.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.analytics.ErrorReporter
import bose.ankush.home.domain.location.LocationProblem
import bose.ankush.home.domain.model.HomeWeather
import bose.ankush.home.domain.remoteconfig.HomeRemoteConfigGate
import bose.ankush.home.domain.usecase.GetActiveCoordinates
import bose.ankush.home.domain.usecase.ObserveHomeWeather
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.default_coordinates_txt
import bose.ankush.home.generated.resources.general_error_txt
import bose.ankush.home.generated.resources.location_permission_denied_txt
import bose.ankush.home.presentation.util.errorMessageFromException
import bose.ankush.network.util.NetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Home's MVI ViewModel. Weather, place, and location all come from [ObserveHomeWeather], which
 * also refetches when a place is pinned from a sibling tab (via
 * [bose.ankush.home.HomeLocationCoordinator]). This class turns that into [HomeState] and
 * handles the notification banner.
 */
internal class HomeViewModel(
    private val observeHomeWeather: ObserveHomeWeather,
    private val getActiveCoordinates: GetActiveCoordinates,
    private val remoteConfigGate: HomeRemoteConfigGate,
    private val analyticsTracker: AnalyticsTracker,
    private val errorReporter: ErrorReporter,
    private val errorMessage: suspend (Exception) -> String? = ::errorMessageFromException,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    private var notificationBannerVisibilityJob: Job? = null
    private var reportedFailure: Throwable? = null

    private val refreshTrigger = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val dataFetchExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e !is CancellationException) {
            viewModelScope.launch {
                val message =
                    if (e is Exception) errorMessage(e) else getString(Res.string.general_error_txt)
                dispatch(HomeAction.Error(message))
            }
        }
    }

    init {
        remoteConfigGate.initialize()

        viewModelScope.launch(dataFetchExceptionHandler) {
            observeHomeWeather(refreshTrigger).collect { weather ->
                reportFailure(weather.status)
                dispatch(HomeAction.WeatherChanged(weather, messageFor(weather)))
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
                        if (_state.value.notificationBanner.isPermanentlyDeclined) {
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

    /**
     * Files each failed attempt once. The toast shows a friendly line; the report keeps the cause
     * and its code, which is what an "Oops!..Something went wrong." needs to be traced.
     */
    private fun reportFailure(status: HomeWeather.Status) {
        val cause = (status as? HomeWeather.Status.Failed)?.cause ?: return
        if (cause === reportedFailure) return
        reportedFailure = cause
        errorReporter.recordError(
            throwable = cause,
            message = "Home weather attempt failed",
            context = mapOf(
                "errorCode" to ((cause as? NetworkException)?.errorCode?.toString() ?: "none")
            ),
        )
    }

    /** Text for a failure or a missing location, or null when [weather] has neither. */
    private suspend fun messageFor(weather: HomeWeather): String? =
        when (val status = weather.status) {
            is HomeWeather.Status.Failed -> messageFrom(status.cause)
            HomeWeather.Status.NoLocation ->
                weather.locationProblem?.toMessage()
                    ?: getString(Res.string.default_coordinates_txt)

            else -> null
        }

    private fun resetLocationOverride() {
        val current = _state.value
        if (current.isResettingLocation) return
        current.pinnedPlaceName?.let { dispatch(HomeAction.ResetLocationStarted(it)) }
        viewModelScope.launch(dataFetchExceptionHandler) {
            try {
                getActiveCoordinates.reset()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dispatch(HomeAction.ResetLocationFailed)
                throw e
            }
        }
    }

    private suspend fun LocationProblem.toMessage(): String =
        when (this) {
            LocationProblem.PermissionDenied -> getString(Res.string.location_permission_denied_txt)
            is LocationProblem.GpsDisabled -> messageFrom(cause)
            is LocationProblem.Failed -> messageFrom(cause)
        }

    private suspend fun messageFrom(cause: Throwable): String =
        (cause as? Exception)?.let { errorMessage(it) }
            ?: getString(Res.string.general_error_txt)
}
