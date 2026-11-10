package bose.ankush.home.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import bose.ankush.commonui.components.NotificationToast
import bose.ankush.commonui.components.ToastType
import bose.ankush.commonui.permissions.PermissionAlertDialog
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.cancel_btn_txt
import bose.ankush.home.generated.resources.enable_gps_btn_txt
import bose.ankush.home.generated.resources.enable_notification_btn
import bose.ankush.home.generated.resources.network_unavailable_txt
import bose.ankush.home.generated.resources.notification_permission_declined_txt
import bose.ankush.home.generated.resources.notification_permission_message
import bose.ankush.home.generated.resources.offline_toast_title_txt
import bose.ankush.home.generated.resources.open_settings_btn
import bose.ankush.home.generated.resources.retry_btn_txt
import bose.ankush.home.presentation.HomeEffect
import bose.ankush.home.presentation.HomeIntent
import bose.ankush.home.presentation.HomeState
import bose.ankush.home.presentation.HomeViewModel
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Actions the classic home already had. Defaults keep the preview on the plain shell.
 */
data class HomeScreenChrome(
    val current: WeatherForecast.Current? = null,
    val timezoneOffset: Int? = null,
    val todaySummary: String? = null,
    /** [HomeState.isLoading]: the bottom loader. Pull to refresh only plays its gesture. */
    val loading: Boolean = false,
    val onRefresh: () -> Unit = {},
    val locationOverrideName: String? = null,
    /** Reset to GPS tapped; the chip spins until the GPS forecast lands. */
    val resettingLocation: Boolean = false,
    val onResetLocation: () -> Unit = {},
    val showNotificationPrompt: Boolean = false,
    val notificationDeclinedForever: Boolean = false,
    val onEnableNotifications: () -> Unit = {},
    val onDismissNotifications: () -> Unit = {},
    val offlineMessage: String? = null,
    val forecastLoading: Boolean = false,
    val forecastFailed: Boolean = false,
    val onRetryForecast: () -> Unit = {},
    val eventDates: Set<LocalDate> = emptySet(),
    val gpsDisabled: Boolean = false,
    val locationPermissionDenied: Boolean = false,
    val onEnableGps: () -> Unit = {},
    val onRequestLocationPermission: () -> Unit = {},
)

@Composable
internal fun rememberHomeScreenChrome(
    state: HomeState,
    eventDates: Set<LocalDate>,
    forecastVisible: Boolean,
    viewModel: HomeViewModel,
): HomeScreenChrome {
    val context = LocalContext.current
    // One instance per input change, so the pager pages that take it can skip recomposition.
    return remember(state, eventDates, forecastVisible, viewModel, context) {
        HomeScreenChrome(
            current = state.weatherData?.current,
            timezoneOffset = state.weatherData?.timezoneOffset,
            todaySummary =
                state.weatherData
                    ?.daily
                    ?.firstOrNull()
                    ?.summary,
            loading = state.isLoading,
            onRefresh = { viewModel.processIntent(HomeIntent.Refresh) },
            locationOverrideName = state.locationChipName,
            resettingLocation = state.isResettingLocation,
            onResetLocation = { viewModel.processIntent(HomeIntent.ResetLocationOverride) },
            showNotificationPrompt = state.notificationBanner.isVisible,
            notificationDeclinedForever = state.notificationBanner.isPermanentlyDeclined,
            onEnableNotifications = { viewModel.processIntent(HomeIntent.EnableNotificationBanner) },
            onDismissNotifications = { viewModel.processIntent(HomeIntent.DismissNotificationBanner) },
            offlineMessage = if (forecastVisible && state.isOffline) state.offlineMessage else null,
            // No week shimmer until Room is known to be empty, so a cached reload never flashes it.
            forecastLoading = state.isLoading && !forecastVisible && state.hasCheckedCache,
            forecastFailed = !forecastVisible && !state.isLoading && (state.error != null || state.isOffline),
            onRetryForecast = { viewModel.processIntent(HomeIntent.FetchLocation) },
            eventDates = eventDates,
            gpsDisabled = state.isGpsDisabled,
            locationPermissionDenied = state.isLocationPermissionDenied,
            onEnableGps = { context.openLocationSettings() },
            onRequestLocationPermission = { viewModel.processIntent(HomeIntent.RequestLocationPermission) },
        )
    }
}

@Composable
internal fun CollectHomeEffects(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var askedNotification by remember { mutableStateOf(false) }
    var hadLocation by remember { mutableStateOf(context.hasLocationPermission()) }
    LaunchedEffect(Unit) {
        viewModel.processIntent(HomeIntent.UpdateNotificationPermissionState(context.notificationsGranted()))
    }
    DisposableEffect(lifecycle) {
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
                val locationGranted = context.hasLocationPermission()
                if (locationGranted && !hadLocation) {
                    viewModel.processIntent(HomeIntent.FetchLocation)
                }
                hadLocation = locationGranted
                val granted = context.notificationsGranted()
                if (askedNotification) {
                    val activity = context.findActivity()
                    val permanent =
                        !granted &&
                            activity != null &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                    viewModel.processIntent(HomeIntent.NotificationPermissionResult(granted, permanent))
                    askedNotification = false
                } else {
                    viewModel.processIntent(HomeIntent.UpdateNotificationPermissionState(granted))
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel.effect) {
        viewModel.effect.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect { effect ->
            when (effect) {
                HomeEffect.RequestNotificationPermission -> {
                    askedNotification = true
                    val activity = context.findActivity()
                    if (activity == null) {
                        context.openAppSettings()
                    } else {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_REQUEST,
                        )
                    }
                }
                HomeEffect.OpenNotificationSettings -> context.openNotificationSettings()
                HomeEffect.RequestLocationPermission -> {
                    val activity = context.findActivity()
                    if (activity == null) {
                        context.openAppSettings()
                    } else {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                            LOCATION_REQUEST,
                        )
                    }
                }
                HomeEffect.RequestGpsPermission -> context.openLocationSettings()
            }
        }
    }
}

@Composable
internal fun NotificationPrompt(chrome: HomeScreenChrome) {
    if (!chrome.showNotificationPrompt) return
    // Once declined for good the system dialog never shows again, so say where the tap goes.
    val declined = chrome.notificationDeclinedForever
    PermissionAlertDialog(
        descriptionText =
            stringResource(
                if (declined) {
                    Res.string.notification_permission_declined_txt
                } else {
                    Res.string.notification_permission_message
                },
            ),
        isPermanentlyDeclined = declined,
        onPositiveAction = chrome.onEnableNotifications,
        onNegativeAction = chrome.onDismissNotifications,
        positiveButtonLabel =
            stringResource(if (declined) Res.string.open_settings_btn else Res.string.enable_notification_btn),
        negativeButtonLabel = stringResource(Res.string.cancel_btn_txt),
    )
}

@Composable
/** Place at the bottom: the toast only fills its width and keeps its own gap above the tab bar. */
internal fun OfflineToast(
    message: String?,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message.isNullOrBlank()) {
            visible = false
        } else {
            text = message
            visible = true
        }
    }
    NotificationToast(
        modifier = modifier,
        message = text ?: stringResource(Res.string.network_unavailable_txt),
        title = stringResource(Res.string.offline_toast_title_txt),
        type = ToastType.WARNING,
        isVisible = visible,
        onDismiss = { visible = false },
        bottomOffset = TabBarReserveHeight,
    )
}

@Composable
internal fun ForecastWaitingActions(
    chrome: HomeScreenChrome,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (chrome.gpsDisabled) {
            ActionLabel(
                label = stringResource(Res.string.enable_gps_btn_txt),
                onClick = chrome.onEnableGps,
                contentColor = contentColor,
            )
        }
        if (chrome.locationPermissionDenied) {
            ActionLabel(
                label = REQUEST_LOCATION,
                onClick = chrome.onRequestLocationPermission,
                contentColor = contentColor,
            )
        }
        if (chrome.forecastFailed) {
            ActionLabel(
                label = stringResource(Res.string.retry_btn_txt),
                onClick = chrome.onRetryForecast,
                contentColor = contentColor,
            )
            ActionLabel(
                label = RetryLabels.FORECAST,
                onClick = chrome.onRetryForecast,
                contentColor = contentColor,
            )
        }
    }
}

private fun Context.notificationsGranted(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.hasLocationPermission(): Boolean =
    listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ).all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

/** This app's notification switches, not the general app info page. */
private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

private fun Context.openLocationSettings() {
    startActivity(
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private const val NOTIFICATION_REQUEST = 4102
private const val LOCATION_REQUEST = 4103
private const val CARD_ALPHA = 0.38f
private const val REQUEST_LOCATION = "Request location permission"
