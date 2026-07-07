package bose.ankush.home

/** Fed back into [bose.ankush.home.presentation.HomeFeatureRoute] after the host launches the
 * platform notification-permission dialog in response to [bose.ankush.home.presentation.HomeEffect.RequestNotificationPermission]. */
data class HomeNotificationPermissionResult(
    val isGranted: Boolean,
    val isPermanentlyDeclined: Boolean,
)
