@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun rememberPlatformPermissions(): PlatformPermissions = remember { IosPlatformPermissions() }

private class IosPlatformPermissions : PlatformPermissions {
    private val locationManager = CLLocationManager()
    private var cachedHasNotificationPermission by mutableStateOf(false)

    override fun hasLocationPermission(): Boolean {
        val status = locationManager.authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedAlways ||
            status == kCLAuthorizationStatusAuthorizedWhenInUse
    }

    override fun hasNotificationPermission(): Boolean {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            cachedHasNotificationPermission =
                settings?.authorizationStatus == UNAuthorizationStatusAuthorized ||
                settings?.authorizationStatus == UNAuthorizationStatusProvisional
        }
        return cachedHasNotificationPermission
    }

    override fun requiresRuntimeNotificationPermission(): Boolean = true

    // iOS has exposed per-app language via the general Settings page since iOS 13.
    override fun supportsPerAppLocaleSettings(): Boolean = true

    override fun openAppSystemSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    // iOS has no location-specific deep link into Settings; the general app settings page is the
    // closest equivalent and already surfaces the Location permission row.
    override fun openLocationSettings() = openAppSystemSettings()

    // iOS 13+ apps opt into per-app language via the same general Settings page (Language & Region).
    override fun openAppLocaleSettings() = openAppSystemSettings()
}
