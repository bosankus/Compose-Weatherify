package bose.ankush.home.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import bose.ankush.home.data.location.DeviceLocationClient.Companion.MAX_CACHED_FIX_AGE
import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.LocationClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal class DeviceLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationClient {
    private fun checkLocationPermission() {
        if (!context.hasLocationPermission()) {
            throw LocationClient.LocationException(
                message = "Location permission is not granted.",
                reason = LocationClient.Reason.PermissionDenied,
            )
        }
    }

    private fun checkGpsEnabled() {
        if (!isLocationEnabled()) {
            throw LocationClient.LocationException(
                message = "GPS is disabled",
                reason = LocationClient.Reason.ServicesOff,
            )
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isLocationEnabled
    }

    override suspend fun getCurrentLocation(): Result<Coordinates> {
        try {
            checkLocationPermission()
            checkGpsEnabled()
        } catch (e: LocationClient.LocationException) {
            return Result.failure(e)
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
     * The fused provider's last fix, from this app or any other, when no fresh one came. Up to
     * [MAX_CACHED_FIX_AGE] old is still the right forecast: people rarely cross a city that fast.
     */
    @SuppressLint("MissingPermission")
    private suspend fun recentCachedFix(): Coordinates? =
        withTimeoutOrNull(CACHED_FIX_TIMEOUT) {
            suspendCancellableCoroutine { continuation ->
                client.lastLocation
                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                    .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
            }
        }?.takeIf { isAcceptable(it, maxAge = MAX_CACHED_FIX_AGE) }
            ?.let { Coordinates(it.latitude, it.longitude) }

    @SuppressLint("MissingPermission")
    private suspend fun fetchOnce(): Result<Coordinates> {
        val result =
            withTimeoutOrNull(LOCATION_FETCH_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val cts = CancellationTokenSource()
                    var providerReceiver: BroadcastReceiver? = null

                    fun resumeOnce(result: Result<Coordinates>) {
                        if (continuation.isActive) {
                            providerReceiver?.let { context.unregisterReceiver(it) }
                            providerReceiver = null
                            continuation.resume(result)
                        }
                    }

                    providerReceiver =
                        object : BroadcastReceiver() {
                            override fun onReceive(
                                context: Context?,
                                intent: Intent?,
                            ) {
                                if (!isLocationEnabled()) {
                                    cts.cancel()
                                    resumeOnce(
                                        Result.failure(
                                            LocationClient.LocationException(
                                                message = "GPS was disabled while fetching coordinates!",
                                                reason = LocationClient.Reason.ServicesOff,
                                            ),
                                        ),
                                    )
                                }
                            }
                        }

                    ContextCompat.registerReceiver(
                        context,
                        providerReceiver,
                        IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )

                    // A forecast covers kilometres; a balanced fix comes from Wi-Fi and cell in a second or
                    // two, where high accuracy waits on GPS and often times out indoors.
                    client
                        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { location ->
                            when {
                                location == null ->
                                    resumeOnce(
                                        Result.failure(
                                            LocationClient.LocationException(NO_COORDINATES),
                                        ),
                                    )

                                !isAcceptable(location) ->
                                    resumeOnce(
                                        Result.failure(
                                            LocationClient.LocationException(STALE_COORDINATES),
                                        ),
                                    )

                                else ->
                                    resumeOnce(
                                        Result.success(
                                            Coordinates(
                                                location.latitude,
                                                location.longitude,
                                            ),
                                        ),
                                    )
                            }
                        }.addOnFailureListener { e ->
                            resumeOnce(
                                Result.failure(
                                    LocationClient.LocationException(
                                        e.message ?: "Unknown error",
                                    ),
                                ),
                            )
                        }

                    continuation.invokeOnCancellation {
                        cts.cancel()
                        providerReceiver?.let { context.unregisterReceiver(it) }
                        providerReceiver = null
                    }
                }
            }
        return result
            ?: Result.failure(LocationClient.LocationException("Timed out waiting for coordinates"))
    }

    override fun hasLocationPermission(): Boolean = context.hasLocationPermission()

    private fun isAcceptable(
        location: Location,
        maxAge: Duration = MAX_LOCATION_AGE,
    ): Boolean {
        val ageMs = Clock.System.now().toEpochMilliseconds() - location.time
        return ageMs < maxAge.inWholeMilliseconds && location.accuracy <= MAX_ACCURACY_METERS
    }

    private companion object {
        private val LOCATION_FETCH_TIMEOUT_MS = 10_000.milliseconds
        private val CACHED_FIX_TIMEOUT = 2_000.milliseconds
        private val MAX_LOCATION_AGE = 2.minutes
        private val MAX_CACHED_FIX_AGE = 30.minutes

        // Matches GPS_SAME_PLACE_METERS: anything within it gets the same forecast anyway.
        private const val MAX_ACCURACY_METERS = 1_000f
        private const val MAX_ATTEMPTS = 2
        private val RETRY_DELAY_MS = 500.milliseconds
        private const val NO_COORDINATES = "Coordinates are not present."
        private const val STALE_COORDINATES = "Coordinates are not updated!"
        private val TRANSIENT_FAILURE_MESSAGES = setOf(NO_COORDINATES, STALE_COORDINATES)
    }
}

private fun Context.hasLocationPermission(): Boolean =
    listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ).any { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
