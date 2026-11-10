@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.home.data.location

import bose.ankush.home.data.location.IosLocationClient.Companion.MAX_CACHED_FIX_AGE
import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.LocationClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.toKotlinInstant
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLErrorDenied
import platform.CoreLocation.kCLErrorDomain
import platform.CoreLocation.kCLErrorLocationUnknown
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal class IosLocationClient : LocationClient {
    // CLLocationManager must be created and used on the main thread.
    private var manager: CLLocationManager? = null

    // CLLocationManager.delegate is a weak Obj-C property; without a strong Kotlin reference the
    // delegate can be deallocated before didUpdateLocations/didFailWithError ever fires.
    private var activeDelegate: CLLocationManagerDelegateProtocol? = null

    private fun requireManager(): CLLocationManager =
        manager ?: CLLocationManager().apply {
            // A forecast covers kilometres; a 100 m fix arrives in a second or two from Wi-Fi,
            // where "best" waits on GPS and often times out indoors.
            desiredAccuracy = kCLLocationAccuracyHundredMeters
            manager = this
        }

    override fun hasLocationPermission(): Boolean {
        val status =
            manager?.authorizationStatus
                ?: CLLocationManager().authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedAlways ||
            status == kCLAuthorizationStatusAuthorizedWhenInUse
    }

    override suspend fun getCurrentLocation(): Result<Coordinates> {
        if (!hasLocationPermission()) {
            return Result.failure(
                LocationClient.LocationException(
                    message = "Location permission is not given.",
                    reason = LocationClient.Reason.PermissionDenied,
                ),
            )
        }
        // locationServicesEnabled() blocks on a system query and CoreLocation flags it when called
        // on the main thread, so it runs on a background dispatcher.
        val servicesEnabled =
            withContext(Dispatchers.Default) { CLLocationManager.locationServicesEnabled() }
        if (!servicesEnabled) {
            return Result.failure(
                LocationClient.LocationException(
                    message = "Location services are off",
                    reason = LocationClient.Reason.ServicesOff,
                ),
            )
        }

        var result = fetchOnce()
        var attempts = 1
        while (attempts < MAX_ATTEMPTS && result.exceptionOrNull()?.message in TRANSIENT_FAILURE_MESSAGES) {
            delay(RETRY_DELAY_MS)
            result = fetchOnce()
            attempts++
        }
        val noFix = (result.exceptionOrNull() as? LocationClient.LocationException)
            ?.reason == LocationClient.Reason.NoFix
        return if (noFix) recentCachedFix()?.let { Result.success(it) } ?: result else result
    }

    /**
     * CoreLocation's last fix, from this app or any other, when no fresh one came. Up to
     * [MAX_CACHED_FIX_AGE] old is still the right forecast: people rarely cross a city that fast.
     */
    private suspend fun recentCachedFix(): Coordinates? =
        withContext(Dispatchers.Main) {
            requireManager().location
                ?.takeIf { isAcceptable(it, maxAge = MAX_CACHED_FIX_AGE) }
                ?.coordinate
                ?.useContents { Coordinates(latitude, longitude) }
        }

    private suspend fun fetchOnce(): Result<Coordinates> {
        // CLLocationManager APIs must be called on the main thread.
        val result =
            withContext(Dispatchers.Main) {
                withTimeoutOrNull(LOCATION_FETCH_TIMEOUT_MS) {
                    suspendCancellableCoroutine { continuation ->
                        val mgr = requireManager()
                        val delegate =
                            object : NSObject(), CLLocationManagerDelegateProtocol {
                                fun clearIfCurrent() {
                                    if (activeDelegate === this) activeDelegate = null
                                }

                                override fun locationManager(
                                    manager: CLLocationManager,
                                    didUpdateLocations: List<*>,
                                ) {
                                    val location = didUpdateLocations.lastOrNull() as? CLLocation
                                    manager.stopUpdatingLocation()
                                    clearIfCurrent()
                                    if (!continuation.isActive) return

                                    when {
                                        location == null ->
                                            continuation.resume(
                                                Result.failure(
                                                    LocationClient.LocationException(NO_COORDINATES),
                                                ),
                                            )

                                        !isAcceptable(location) ->
                                            continuation.resume(
                                                Result.failure(
                                                    LocationClient.LocationException(
                                                        STALE_COORDINATES
                                                    ),
                                                ),
                                            )

                                        else -> {
                                            val coordinates =
                                                location.coordinate.useContents { Coordinates(latitude, longitude) }
                                            continuation.resume(Result.success(coordinates))
                                        }
                                    }
                                }

                                override fun locationManager(
                                    manager: CLLocationManager,
                                    didFailWithError: NSError,
                                ) {
                                    manager.stopUpdatingLocation()
                                    clearIfCurrent()
                                    if (continuation.isActive) {
                                        continuation.resume(
                                            Result.failure(
                                                didFailWithError.toLocationException(),
                                            ),
                                        )
                                    }
                                }
                            }

                        activeDelegate = delegate
                        mgr.delegate = delegate
                        mgr.requestLocation()

                        continuation.invokeOnCancellation {
                            mgr.stopUpdatingLocation()
                            delegate.clearIfCurrent()
                        }
                    }
                }
            }
        return result
            ?: Result.failure(LocationClient.LocationException("Timed out waiting for coordinates"))
    }

    private fun NSError.toLocationException(): LocationClient.LocationException =
        when {
            domain == kCLErrorDomain && code == kCLErrorDenied ->
                LocationClient.LocationException(
                    message = localizedDescription,
                    reason = LocationClient.Reason.PermissionDenied,
                )
            // "Unable to obtain a location right now": CoreLocation keeps trying, so it's worth one more go.
            domain == kCLErrorDomain && code == kCLErrorLocationUnknown ->
                LocationClient.LocationException(LOCATION_UNKNOWN)

            else -> LocationClient.LocationException(localizedDescription)
        }

    private fun isAcceptable(
        location: CLLocation,
        maxAge: Duration = MAX_LOCATION_AGE,
    ): Boolean {
        val age = Clock.System.now() - location.timestamp.toKotlinInstant()
        return age < maxAge && location.horizontalAccuracy in 0.0..MAX_ACCURACY_METERS
    }

    private companion object {
        private val LOCATION_FETCH_TIMEOUT_MS = 10_000.milliseconds
        private val MAX_LOCATION_AGE = 2.minutes
        private val MAX_CACHED_FIX_AGE = 30.minutes

        // Matches GPS_SAME_PLACE_METERS: anything within it gets the same forecast anyway.
        private const val MAX_ACCURACY_METERS = 1_000.0
        private const val MAX_ATTEMPTS = 2
        private val RETRY_DELAY_MS = 500.milliseconds
        private const val NO_COORDINATES = "Coordinates are not present."
        private const val STALE_COORDINATES = "Coordinates are not updated!"
        private const val LOCATION_UNKNOWN = "Location unknown"
        private val TRANSIENT_FAILURE_MESSAGES =
            setOf(NO_COORDINATES, STALE_COORDINATES, LOCATION_UNKNOWN)
    }
}
