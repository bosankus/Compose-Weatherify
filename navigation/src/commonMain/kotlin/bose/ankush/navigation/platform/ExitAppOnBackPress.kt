package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable

/**
 * Exits the app when back is pressed while on the start-route tab with a single back-stack entry.
 * No-op on platforms without a hardware/gesture "back to exit app" concept (e.g. iOS).
 */
@Composable
expect fun ExitAppOnBackPress(enabled: Boolean = true)
