@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

@Composable
actual fun rememberPlatformPermissions(): PlatformPermissions = remember { IosPlatformPermissions() }

private class IosPlatformPermissions : PlatformPermissions {
    private val locationManager = CLLocationManager()

    override fun hasLocationPermission(): Boolean {
        val status = locationManager.authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedAlways ||
            status == kCLAuthorizationStatusAuthorizedWhenInUse
    }

    // Queried fresh on every call — the status can change behind the app's back (e.g. the user
    // flips it in Settings), so caching it previously caused the permission prompt/banner to
    // reappear even after the user had granted it.
    override suspend fun hasNotificationPermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
                val granted =
                    settings?.authorizationStatus == UNAuthorizationStatusAuthorized ||
                        settings?.authorizationStatus == UNAuthorizationStatusProvisional
                continuation.resume(granted)
            }
        }

    override fun requiresRuntimeNotificationPermission(): Boolean = true

    // iOS has exposed per-app language via the general Settings page since iOS 13.
    override fun supportsPerAppLocaleSettings(): Boolean = true

    // The Location/Notifications row lives inline on the app's settings page, but the user still
    // has to tap into it (e.g. pick "While Using the App", or toggle "Allow Notifications") — no
    // public API can land them past this point.
    override fun requiresManualSettingsNavigationHint(): Boolean = true

    override fun openAppSystemSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        // The no-completion-handler openURL: overload is deprecated (iOS 10+) and unreliable through
        // K/N's Obj-C interop; use the options/completionHandler overload Apple recommends instead.
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }

    // iOS has no location-specific deep link into Settings; the general app settings page is the
    // closest equivalent and already surfaces the Location permission row.
    override fun openLocationSettings() = openAppSystemSettings()

    // iOS 13+ apps opt into per-app language via the same general Settings page (Language & Region).
    override fun openAppLocaleSettings() = openAppSystemSettings()
}
