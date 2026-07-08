package bose.ankush.home.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import bose.ankush.commonui.components.NotificationToast
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.components.ToastType
import bose.ankush.commonui.permissions.PermissionAlertDialog
import bose.ankush.home.HomeNotificationPermissionResult
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.cancel_btn_txt
import bose.ankush.home.generated.resources.enable_gps_btn_txt
import bose.ankush.home.generated.resources.enable_notification_btn
import bose.ankush.home.generated.resources.location_override_chip_content_desc
import bose.ankush.home.generated.resources.location_override_reset_btn
import bose.ankush.home.generated.resources.network_unavailable_txt
import bose.ankush.home.generated.resources.notification_permission_message
import bose.ankush.home.generated.resources.offline_toast_title_txt
import bose.ankush.home.generated.resources.retry_btn_txt
import bose.ankush.home.presentation.component.BriefAirQualityReportCardLayout
import bose.ankush.home.presentation.component.CurrentWeatherReportLayout
import bose.ankush.home.presentation.component.DailyWeatherForecastReportLayout
import bose.ankush.home.presentation.component.HourlyWeatherForecastReportLayout
import bose.ankush.home.presentation.component.SunriseSunsetCombinedAnimation
import bose.ankush.home.presentation.component.WeatherAlertLayout
import bose.ankush.home.presentation.state.ErrorBackgroundAnimation
import bose.ankush.home.presentation.state.ShowError
import bose.ankush.home.presentation.state.ShowLoading
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private const val ANIMATION_INITIAL_DELAY_MS = 100L
private const val ANIMATION_STAGGER_DELAY_MS = 150L

/**
 * Public entry point for the Home feature. Bridges to the host app via plain callbacks/params —
 * [HomeViewModel] itself stays internal, resolved through Koin and scoped to wherever this route
 * is composed (mirrors `feature:finder`'s `SavedLocationsFinderRoute`).
 */
@Composable
fun HomeFeatureRoute(
    bottomBar: @Composable () -> Unit = {},
    toastAnchorState: ToastAnchorState? = null,
    hasLocationPermission: Boolean = true,
    hasNotificationPermission: Boolean = true,
    notificationPermissionResult: HomeNotificationPermissionResult? = null,
    onRequestNotificationPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var previousHasLocationPermission by remember { mutableStateOf(hasLocationPermission) }
    LaunchedEffect(hasLocationPermission) {
        // Only re-fetch on the granted transition — the ViewModel's own init already handles the
        // case where permission was already granted when this route was first composed.
        if (hasLocationPermission && !previousHasLocationPermission) {
            viewModel.processIntent(HomeIntent.FetchLocation)
        }
        previousHasLocationPermission = hasLocationPermission
    }

    LaunchedEffect(hasNotificationPermission) {
        viewModel.processIntent(
            HomeIntent.UpdateNotificationPermissionState(
                hasNotificationPermission,
            ),
        )
    }

    LaunchedEffect(notificationPermissionResult) {
        notificationPermissionResult?.let {
            viewModel.processIntent(
                HomeIntent.NotificationPermissionResult(it.isGranted, it.isPermanentlyDeclined),
            )
        }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
                when (effect) {
                    HomeEffect.RequestNotificationPermission -> onRequestNotificationPermission()
                    HomeEffect.OpenSettings -> onOpenSettings()
                    HomeEffect.RequestGpsPermission -> TODO()
                    HomeEffect.RequestLocationPermission -> TODO()
                }
            }
    }

    when {
        state.weatherData
            ?.current
            ?.weather
            ?.isNotEmpty() == true || state.airQualityData != null -> {
            ShowUIContainer(
                state = state,
                bottomBar = bottomBar,
                toastAnchorState = toastAnchorState,
                onRefresh = { viewModel.processIntent(HomeIntent.Refresh) },
                onResetLocationOverride = { viewModel.processIntent(HomeIntent.ResetLocationOverride) },
                onEnableNotifications = { viewModel.processIntent(HomeIntent.EnableNotificationBanner) },
                onDismissNotificationBanner = { viewModel.processIntent(HomeIntent.DismissNotificationBanner) },
            )
        }

        !state.error.isNullOrEmpty() -> {
            HandleScreenError(
                errorText = state.error,
                isLoading = state.isLoading,
                isGpsDisabled = state.isGpsDisabled,
                onOpenLocationSettings = onOpenLocationSettings,
                onRetry = { viewModel.processIntent(HomeIntent.FetchLocation) },
            )
        }

        else ->
            ShowLoading(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
            )
    }
}

@Composable
private fun HandleScreenError(
    errorText: String?,
    isLoading: Boolean,
    isGpsDisabled: Boolean,
    onOpenLocationSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ErrorBackgroundAnimation()

        ShowError(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(all = 16.dp),
            msg = errorText,
            buttonText =
                if (isGpsDisabled) {
                    stringResource(Res.string.enable_gps_btn_txt)
                } else {
                    stringResource(Res.string.retry_btn_txt)
                },
            isLoading = isLoading,
            buttonAction = if (isGpsDisabled) onOpenLocationSettings else onRetry,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowUIContainer(
    state: HomeState,
    bottomBar: @Composable () -> Unit,
    toastAnchorState: ToastAnchorState?,
    onRefresh: () -> Unit,
    onResetLocationOverride: () -> Unit,
    onEnableNotifications: () -> Unit,
    onDismissNotificationBanner: () -> Unit,
) {
    val weatherReports = state.weatherData
    val airQualityReports = state.airQualityData

    var showOfflineToast by remember { mutableStateOf(false) }
    LaunchedEffect(state.isOffline) {
        if (state.isOffline) showOfflineToast = true
    }

    val pullToRefreshState = rememberPullToRefreshState()

    val currentWeatherTransitionState = remember { MutableTransitionState(false) }
    val alertsTransitionState = remember { MutableTransitionState(false) }
    val airQualityTransitionState = remember { MutableTransitionState(false) }
    val hourlyForecastTransitionState = remember { MutableTransitionState(false) }
    val dailyForecastTransitionState = remember { MutableTransitionState(false) }

    LaunchedEffect(weatherReports, airQualityReports) {
        currentWeatherTransitionState.targetState = false
        alertsTransitionState.targetState = false
        airQualityTransitionState.targetState = false
        hourlyForecastTransitionState.targetState = false
        dailyForecastTransitionState.targetState = false

        delay(ANIMATION_INITIAL_DELAY_MS.milliseconds)
        currentWeatherTransitionState.targetState = true

        delay(ANIMATION_STAGGER_DELAY_MS.milliseconds)
        alertsTransitionState.targetState = true

        delay(ANIMATION_STAGGER_DELAY_MS.milliseconds)
        airQualityTransitionState.targetState = true

        delay(ANIMATION_STAGGER_DELAY_MS.milliseconds)
        hourlyForecastTransitionState.targetState = true

        delay(ANIMATION_STAGGER_DELAY_MS.milliseconds)
        dailyForecastTransitionState.targetState = true
    }

    Box {
        weatherReports?.current?.let { currentWeather ->
            SunriseSunsetCombinedAnimation(
                sunriseTimestamp = currentWeather.sunrise,
                sunsetTimestamp = currentWeather.sunset,
                currentTimestamp = Clock.System.now().epochSeconds,
            )
        }

        if (state.showNotificationBanner) {
            PermissionAlertDialog(
                descriptionText = stringResource(Res.string.notification_permission_message),
                isPermanentlyDeclined = state.isNotificationPermissionPermanentlyDeclined,
                onPositiveAction = onEnableNotifications,
                onNegativeAction = onDismissNotificationBanner,
                positiveButtonLabel = stringResource(Res.string.enable_notification_btn),
                negativeButtonLabel = stringResource(Res.string.cancel_btn_txt),
            )
        }

        NotificationToast(
            modifier = Modifier.align(Alignment.BottomCenter),
            message = state.offlineMessage ?: stringResource(Res.string.network_unavailable_txt),
            title = stringResource(Res.string.offline_toast_title_txt),
            type = ToastType.WARNING,
            isVisible = showOfflineToast,
            onDismiss = { showOfflineToast = false },
            anchorState = toastAnchorState,
        )

        Scaffold(
            containerColor = Color.Transparent,
            content = { innerPadding ->
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = rememberLazyListState(),
                    ) {
                        if (state.isLocationOverridden && state.activeLocationName != null) {
                            item(key = "location_override_chip") {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AssistChip(
                                        onClick = onResetLocationOverride,
                                        label = {
                                            Text(
                                                text =
                                                    "${state.activeLocationName}  ·  ${
                                                        stringResource(Res.string.location_override_reset_btn)
                                                    }",
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription =
                                                    stringResource(
                                                        Res.string.location_override_chip_content_desc,
                                                        state.activeLocationName,
                                                    ),
                                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                                            )
                                        },
                                        colors =
                                            AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                leadingIconContentColor =
                                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                            ),
                                    )
                                }
                            }
                        }

                        item(key = "current_weather") {
                            weatherReports?.current?.let {
                                AnimatedVisibility(
                                    visibleState = currentWeatherTransitionState,
                                    enter =
                                        fadeIn(animationSpec = tween(durationMillis = 500)) +
                                            slideInVertically(
                                                animationSpec = tween(durationMillis = 500),
                                                initialOffsetY = { it / 3 },
                                            ),
                                    exit = fadeOut(),
                                ) {
                                    CurrentWeatherReportLayout(
                                        it,
                                        state.userLocation,
                                        weatherReports.daily?.firstOrNull()?.summary,
                                    )
                                }
                            }
                        }

                        item(key = "weather_alerts") {
                            weatherReports?.alerts?.let { alerts ->
                                AnimatedVisibility(
                                    visibleState = alertsTransitionState,
                                    enter =
                                        fadeIn(animationSpec = tween(durationMillis = 500)) +
                                            slideInVertically(
                                                animationSpec = tween(durationMillis = 500),
                                                initialOffsetY = { it / 3 },
                                            ),
                                    exit = fadeOut(),
                                ) {
                                    WeatherAlertLayout(alerts = alerts)
                                }
                            }
                        }

                        item(key = "air_quality") {
                            airQualityReports?.takeIf { it.aqi > 0 }?.let { aq ->
                                AnimatedVisibility(
                                    visibleState = airQualityTransitionState,
                                    enter =
                                        fadeIn(animationSpec = tween(durationMillis = 500)) +
                                            slideInVertically(
                                                animationSpec = tween(durationMillis = 500),
                                                initialOffsetY = { it / 3 },
                                            ),
                                    exit = fadeOut(),
                                ) {
                                    BriefAirQualityReportCardLayout(aq)
                                }
                            }
                        }

                        item(key = "hourly_forecast") {
                            weatherReports?.hourly?.let {
                                AnimatedVisibility(
                                    visibleState = hourlyForecastTransitionState,
                                    enter =
                                        fadeIn(animationSpec = tween(durationMillis = 500)) +
                                            slideInVertically(
                                                animationSpec = tween(durationMillis = 500),
                                                initialOffsetY = { it / 3 },
                                            ),
                                    exit = fadeOut(),
                                ) {
                                    HourlyWeatherForecastReportLayout(it)
                                }
                            }
                        }

                        item(key = "daily_forecast") {
                            weatherReports?.daily?.let { list ->
                                AnimatedVisibility(
                                    visibleState = dailyForecastTransitionState,
                                    enter =
                                        fadeIn(animationSpec = tween(durationMillis = 500)) +
                                            slideInVertically(
                                                animationSpec = tween(durationMillis = 500),
                                                initialOffsetY = { it / 3 },
                                            ),
                                    exit = fadeOut(),
                                ) {
                                    DailyWeatherForecastReportLayout(list)
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = { bottomBar() },
        )
    }
}
