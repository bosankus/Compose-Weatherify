package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable

/**
 * Invokes [onForeground] whenever the app returns to the foreground at the OS level (e.g. the
 * user backgrounds the app to grant a permission in system Settings, then switches back).
 *
 * This exists because Compose Multiplatform's iOS `LocalLifecycleOwner` is driven by the hosting
 * `UIViewController`'s `viewDidAppear`/`viewWillDisappear`, not by
 * `UIApplicationDidBecomeActiveNotification` — so `ON_RESUME` does NOT fire on a Settings
 * round-trip, since the view controller itself never disappears. Without this, permission state
 * refreshed only on `ON_RESUME` (see `AppNavigation.kt`) goes stale after such a round-trip,
 * which previously caused the notification-permission dialog to keep showing its
 * "go to Settings" copy even after the user had granted the permission there.
 */
@Composable
expect fun ObserveAppForeground(onForeground: () -> Unit)
