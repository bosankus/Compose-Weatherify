package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable

/** Runtime-permission checks and settings-navigation actions the nav shell needs per platform. */
interface PlatformPermissions {
    fun hasLocationPermission(): Boolean

    /**
     * On iOS this reads `UNUserNotificationCenter`'s authorization status, which is only
     * obtainable asynchronously — hence `suspend`. Do not cache the result across calls: the
     * status can change behind the app's back (e.g. the user flips it in Settings), and a stale
     * cache is what previously caused the notification prompt/banner to reappear even after the
     * user had granted the permission.
     */
    suspend fun hasNotificationPermission(): Boolean

    /** Whether this platform requires an explicit runtime request before posting notifications. */
    fun requiresRuntimeNotificationPermission(): Boolean

    /** Whether the OS exposes a native per-app language settings screen [openAppLocaleSettings] can deep-link into. */
    fun supportsPerAppLocaleSettings(): Boolean

    /**
     * Whether [openAppSystemSettings] can only land on the app's general settings page, not a
     * permission-specific sub-page — so declined-permission copy needs to spell out the extra
     * manual steps (e.g. Settings > Apps > this app > Location/Notifications) once the user gets
     * there.
     */
    fun requiresManualSettingsNavigationHint(): Boolean = false

    fun openAppSystemSettings()

    fun openLocationSettings()

    fun openAppLocaleSettings()
}

/** Obtains a [PlatformPermissions] bound to the current platform's UI context (e.g. Android's `Context`). */
@Composable
expect fun rememberPlatformPermissions(): PlatformPermissions
