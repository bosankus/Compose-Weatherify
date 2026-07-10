package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable

/**
 * Launches the platform notification-permission prompt once, then reports the result.
 * Shared by Home's notification banner and the Settings screen's notification nav item — both
 * just need the platform permission dialog launched; the result is optional to consume.
 */
@Composable
expect fun RequestNotificationPermission(onResult: (isGranted: Boolean, isPermanentlyDeclined: Boolean) -> Unit)
