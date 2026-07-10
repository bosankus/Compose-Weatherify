package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable

/** Runtime-permission checks and settings-navigation actions the nav shell needs per platform. */
interface PlatformPermissions {
    fun hasLocationPermission(): Boolean

    fun hasNotificationPermission(): Boolean

    /** Whether this platform requires an explicit runtime request before posting notifications. */
    fun requiresRuntimeNotificationPermission(): Boolean

    /** Whether the OS exposes a native per-app language settings screen [openAppLocaleSettings] can deep-link into. */
    fun supportsPerAppLocaleSettings(): Boolean

    fun openAppSystemSettings()

    fun openLocationSettings()

    fun openAppLocaleSettings()
}

/** Obtains a [PlatformPermissions] bound to the current platform's UI context (e.g. Android's `Context`). */
@Composable
expect fun rememberPlatformPermissions(): PlatformPermissions
