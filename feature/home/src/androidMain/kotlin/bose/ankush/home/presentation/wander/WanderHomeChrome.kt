package bose.ankush.home.presentation.wander

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import bose.ankush.home.generated.resources.location_override_chip_content_desc
import bose.ankush.home.generated.resources.location_override_reset_btn
import bose.ankush.home.generated.resources.network_unavailable_txt
import bose.ankush.home.generated.resources.notification_permission_message
import bose.ankush.home.generated.resources.offline_toast_title_txt
import bose.ankush.home.generated.resources.retry_btn_txt
import bose.ankush.home.presentation.HomeEffect
import bose.ankush.home.presentation.HomeIntent
import bose.ankush.home.presentation.HomeState
import bose.ankush.home.presentation.HomeViewModel
import bose.ankush.home.presentation.shell.ShellSectionKind
import bose.ankush.home.presentation.shell.ShellState
import bose.ankush.home.presentation.shell.retryContentDescription
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Actions the classic home already had. Defaults keep the preview on the plain shell.
 */
data class WanderHomeChrome(
    val current: WeatherForecast.Current? = null,
    val todaySummary: String? = null,
    val refreshing: Boolean = false,
    val onRefresh: () -> Unit = {},
    val locationOverrideName: String? = null,
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
internal fun rememberWanderChrome(
    state: HomeState,
    shellState: ShellState,
    forecastVisible: Boolean,
    viewModel: HomeViewModel,
): WanderHomeChrome {
    val context = LocalContext.current
    return WanderHomeChrome(
        current = state.weatherData?.current,
        todaySummary =
            state.weatherData
                ?.daily
                ?.firstOrNull()
                ?.summary,
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.processIntent(HomeIntent.Refresh) },
        locationOverrideName = state.activeLocationName?.takeIf { state.isLocationOverridden && it.isNotBlank() },
        onResetLocation = { viewModel.processIntent(HomeIntent.ResetLocationOverride) },
        showNotificationPrompt = state.showNotificationBanner,
        notificationDeclinedForever = state.isNotificationPermissionPermanentlyDeclined,
        onEnableNotifications = { viewModel.processIntent(HomeIntent.EnableNotificationBanner) },
        onDismissNotifications = { viewModel.processIntent(HomeIntent.DismissNotificationBanner) },
        offlineMessage = if (forecastVisible && state.isOffline) state.offlineMessage else null,
        forecastLoading = state.isLoading && !forecastVisible,
        forecastFailed = !forecastVisible && !state.isLoading && (state.error != null || state.isOffline),
        onRetryForecast = { viewModel.processIntent(HomeIntent.FetchLocation) },
        eventDates = shellState.eventDates,
        gpsDisabled = state.isGpsDisabled,
        locationPermissionDenied = state.isLocationPermissionDenied,
        onEnableGps = { context.openLocationSettings() },
        onRequestLocationPermission = { viewModel.processIntent(HomeIntent.RequestLocationPermission) },
    )
}

@Composable
internal fun WanderCollectHomeEffects(viewModel: HomeViewModel) {
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
                HomeEffect.OpenSettings -> context.openAppSettings()
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
internal fun WanderNotificationPrompt(chrome: WanderHomeChrome) {
    if (!chrome.showNotificationPrompt) return
    PermissionAlertDialog(
        descriptionText = stringResource(Res.string.notification_permission_message),
        isPermanentlyDeclined = chrome.notificationDeclinedForever,
        onPositiveAction = chrome.onEnableNotifications,
        onNegativeAction = chrome.onDismissNotifications,
        positiveButtonLabel = stringResource(Res.string.enable_notification_btn),
        negativeButtonLabel = stringResource(Res.string.cancel_btn_txt),
    )
}

@Composable
internal fun WanderOfflineToast(message: String?) {
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
        message = text ?: stringResource(Res.string.network_unavailable_txt),
        title = stringResource(Res.string.offline_toast_title_txt),
        type = ToastType.WARNING,
        isVisible = visible,
        onDismiss = { visible = false },
    )
}

@Composable
internal fun WanderLocationChip(
    label: String,
    name: String,
    onReset: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.location_override_chip_content_desc, name)
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = CARD_ALPHA))
                .semantics {
                    role = Role.Button
                    contentDescription = description
                }.clickable(onClick = onReset)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.padding(0.dp),
        )
        Text(
            text = "$label  ·  ${stringResource(Res.string.location_override_reset_btn)}",
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun WanderWaitingActions(
    chrome: WanderHomeChrome,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (chrome.gpsDisabled) {
            WanderActionLabel(
                label = stringResource(Res.string.enable_gps_btn_txt),
                onClick = chrome.onEnableGps,
                contentColor = contentColor,
            )
        }
        if (chrome.locationPermissionDenied) {
            WanderActionLabel(
                label = REQUEST_LOCATION,
                onClick = chrome.onRequestLocationPermission,
                contentColor = contentColor,
            )
        }
        if (chrome.forecastFailed) {
            WanderActionLabel(
                label = stringResource(Res.string.retry_btn_txt),
                onClick = chrome.onRetryForecast,
                contentColor = contentColor,
            )
            WanderActionLabel(
                label = retryContentDescription(ShellSectionKind.Forecast),
                onClick = chrome.onRetryForecast,
                contentColor = contentColor,
            )
        }
    }
}

private fun Context.notificationsGranted(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
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
