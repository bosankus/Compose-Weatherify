@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject
import kotlin.coroutines.resume

@Composable
actual fun RequestLocationPermission(onResult: (isGranted: Boolean, isPermanentlyDeclined: Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        val status = LocationPermissionRequester().requestAuthorization()
        val isGranted =
            status == kCLAuthorizationStatusAuthorizedAlways || status == kCLAuthorizationStatusAuthorizedWhenInUse
        // iOS has no rationale/"permanently declined" distinction like Android — once denied, the
        // user can only re-enable it from Settings, so treat any denial as permanently declined.
        onResult(isGranted, !isGranted)
    }
}

/** Bridges CLLocationManager's delegate-based authorization callback into a single suspend call. */
private class LocationPermissionRequester :
    NSObject(),
    CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager().apply { delegate = this@LocationPermissionRequester }
    private var continuation: CancellableContinuation<CLAuthorizationStatus>? = null

    suspend fun requestAuthorization(): CLAuthorizationStatus {
        val current = manager.authorizationStatus
        if (current != kCLAuthorizationStatusNotDetermined) return current
        return suspendCancellableCoroutine { cont ->
            continuation = cont
            manager.requestWhenInUseAuthorization()
        }
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        continuation?.resume(manager.authorizationStatus)
        continuation = null
    }
}
