package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable

/**
 * Exits the app when back is pressed while on the start-route tab with a single back-stack entry.
 * No-op on platforms without a hardware/gesture "back to exit app" concept (e.g. iOS).
 */
@Composable
expect fun ExitAppOnBackPress(enabled: Boolean = true)

/**
 * An action for a negative/decline choice on a mandatory permission gate (e.g. location).
 * No-op on platforms where apps must not programmatically quit (e.g. iOS, per Apple's HIG).
 */
@Composable
expect fun rememberExitAppAction(): () -> Unit
