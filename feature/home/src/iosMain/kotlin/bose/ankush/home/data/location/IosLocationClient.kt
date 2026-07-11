@file:OptIn(ExperimentalForeignApi::class)

package bose.ankush.home.data.location

import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.LocationClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.toKotlinInstant
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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

    override suspend fun getCurrentLocation(): Result<Coordinates> {
        if (!hasLocationPermission()) {
            return Result.failure(LocationClient.LocationException("Location permission is not given."))
        }
        if (!CLLocationManager.locationServicesEnabled()) {
            return Result.failure(LocationClient.LocationException("GPS is disabled"))
        }

        repeat(MAX_ATTEMPTS) { attempt ->
            val result = fetchOnce()
            val isTransient = result.exceptionOrNull()?.message in TRANSIENT_FAILURE_MESSAGES
            val isLastAttempt = attempt == MAX_ATTEMPTS - 1

            if (result.isSuccess || !isTransient || isLastAttempt) {
                return result
            }
            delay(RETRY_DELAY_MS)
        }
        return Result.failure(LocationClient.LocationException("Unable to obtain location"))
    }

    private suspend fun fetchOnce(): Result<Coordinates> {
        val result =
            withTimeoutOrNull(LOCATION_FETCH_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val delegate =
                        object : NSObject(), CLLocationManagerDelegateProtocol {
                            override fun locationManager(
                                manager: CLLocationManager,
                                didUpdateLocations: List<*>,
                            ) {
                                val location = didUpdateLocations.lastOrNull() as? CLLocation
                                manager.stopUpdatingLocation()
                                if (!continuation.isActive) return

                                when {
                                    location == null ->
                                        continuation.resume(
                                            Result.failure(
                                                LocationClient.LocationException("Coordinates are not present."),
                                            ),
                                        )

                                    !isAcceptable(location) ->
                                        continuation.resume(
                                            Result.failure(
                                                LocationClient.LocationException("Coordinates are not updated!"),
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
                                if (continuation.isActive) {
                                    continuation.resume(
                                        Result.failure(
                                            LocationClient.LocationException(didFailWithError.localizedDescription),
                                        ),
                                    )
                                }
                            }
                        }

                    manager.delegate = delegate
                    manager.requestLocation()

                    continuation.invokeOnCancellation { manager.stopUpdatingLocation() }
                }
            }
        return result
            ?: Result.failure(LocationClient.LocationException("Timed out waiting for coordinates"))
    }

    private fun isAcceptable(location: CLLocation): Boolean {
        val age = Clock.System.now() - location.timestamp.toKotlinInstant()
        return age < MAX_LOCATION_AGE_MS &&
            location.horizontalAccuracy in 0.0..MAX_ACCURACY_METERS
    }

    private companion object {
        private val LOCATION_FETCH_TIMEOUT_MS = 8000.milliseconds
        private val MAX_LOCATION_AGE_MS = 2.minutes
        private const val MAX_ACCURACY_METERS = 200.0
        private const val MAX_ATTEMPTS = 2
        private val RETRY_DELAY_MS = 500.milliseconds
        private val TRANSIENT_FAILURE_MESSAGES =
            setOf(
                "Coordinates are not present.",
                "Coordinates are not updated!",
            )
    }
}
