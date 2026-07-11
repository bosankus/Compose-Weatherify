package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable

/**
 * Launches the platform location-permission prompt once, then reports whether it was granted and
 * whether a denial is permanent (no more rationale to show, only a Settings deep-link remains).
 */
@Composable
expect fun RequestLocationPermission(onResult: (isGranted: Boolean, isPermanentlyDeclined: Boolean) -> Unit)
