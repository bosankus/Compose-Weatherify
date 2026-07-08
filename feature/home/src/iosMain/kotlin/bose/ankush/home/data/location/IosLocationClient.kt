@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.home.data.location

import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.LocationClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

internal class IosLocationClient : LocationClient {
    private val manager: CLLocationManager by lazy {
        CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyBest
        }
    }

    override fun hasLocationPermission(): Boolean {
        val status = manager.authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedAlways ||
            status == kCLAuthorizationStatusAuthorizedWhenInUse
    }

    override suspend fun getCurrentLocation(): Result<Coordinates> =
        suspendCancellableCoroutine { continuation ->
            if (!hasLocationPermission()) {
                continuation.resume(
                    Result.failure(LocationClient.LocationException("Location permission is not given.")),
                )
                return@suspendCancellableCoroutine
            }
            if (!CLLocationManager.locationServicesEnabled()) {
                continuation.resume(Result.failure(LocationClient.LocationException("GPS is disabled")))
                return@suspendCancellableCoroutine
            }

            val delegate =
                object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(
                        manager: CLLocationManager,
                        didUpdateLocations: List<*>,
                    ) {
                        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                        val coordinates = location.coordinate.useContents { Coordinates(latitude, longitude) }
                        manager.stopUpdatingLocation()
                        if (continuation.isActive) continuation.resume(Result.success(coordinates))
                    }

                    override fun locationManager(
                        manager: CLLocationManager,
                        didFailWithError: NSError,
                    ) {
                        manager.stopUpdatingLocation()
                        if (continuation.isActive) {
                            continuation.resume(
                                Result.failure(LocationClient.LocationException(didFailWithError.localizedDescription)),
                            )
                        }
                    }
                }

            manager.delegate = delegate
            manager.requestLocation()

            continuation.invokeOnCancellation { manager.stopUpdatingLocation() }
        }
}
