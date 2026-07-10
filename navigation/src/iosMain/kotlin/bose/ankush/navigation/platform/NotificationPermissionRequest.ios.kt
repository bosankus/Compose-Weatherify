@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

@Composable
actual fun RequestNotificationPermission(onResult: (isGranted: Boolean, isPermanentlyDeclined: Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        val isGranted =
            suspendCancellableCoroutine<Boolean> { continuation ->
                UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
                    completionHandler = { granted, _ -> continuation.resume(granted) },
                )
            }
        // iOS has no rationale/"permanently declined" distinction like Android — once denied, the
        // user can only re-enable it from Settings, so treat any denial as permanently declined.
        onResult(isGranted, !isGranted)
    }
}
