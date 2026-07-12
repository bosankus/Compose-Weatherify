package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable

// Android's Activity-backed LocalLifecycleOwner already fires ON_RESUME reliably when the app
// returns to the foreground (e.g. after visiting system Settings), so no extra hook is needed
// here. This actual exists solely so iOS can plug into UIApplicationDidBecomeActiveNotification,
// which its view-controller-scoped LocalLifecycleOwner does not surface.
@Composable
actual fun ObserveAppForeground(onForeground: () -> Unit) = Unit
