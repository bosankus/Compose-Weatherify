@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.navigation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject

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

/**
 * Bridges CLLocationManager's delegate-based authorization callback into a single suspend call.
 *
 * CLLocationManager calls `locationManagerDidChangeAuthorization` synchronously when a delegate is
 * assigned — before `suspendCancellableCoroutine` has actually suspended. Using a
 * [CompletableDeferred] avoids the race: the callback can complete the deferred regardless of
 * whether the caller is already awaiting it. If the status is "not determined" we request
 * authorization and wait for the follow-up callback.
 */
private class LocationPermissionRequester :
    NSObject(),
    CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private val result = CompletableDeferred<CLAuthorizationStatus>()
    private var didRequestPermission = false

    suspend fun requestAuthorization(): CLAuthorizationStatus {
        withContext(Dispatchers.Main) {
            manager.delegate = this@LocationPermissionRequester
        }
        return result.await()
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus
        if (status == kCLAuthorizationStatusNotDetermined && !didRequestPermission) {
            didRequestPermission = true
            manager.requestWhenInUseAuthorization()
            return
        }
        result.complete(status)
    }
}
