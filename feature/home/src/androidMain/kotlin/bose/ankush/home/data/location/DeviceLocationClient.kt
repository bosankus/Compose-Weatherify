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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal class DeviceLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationClient {

    private fun checkLocationPermission() {
        if (!context.hasLocationPermission()) {
            throw LocationClient.LocationException("Location permission is not granted.")
        }
    }

    private fun checkGpsEnabled() {
        if (!isLocationEnabled()) {
            throw LocationClient.LocationException(
                message = "GPS is disabled",
                isGpsDisabled = true
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

                    providerReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            if (!isLocationEnabled()) {
                                cts.cancel()
                                resumeOnce(
                                    Result.failure(
                                        LocationClient.LocationException(
                                            message = "GPS was disabled while fetching coordinates!",
                                            isGpsDisabled = true
                                        )
                                    )
                                )
                            }
                        }
                    }

                    ContextCompat.registerReceiver(
                        context,
                        providerReceiver,
                        IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )

                    client
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { location ->
                            when {
                                location == null -> resumeOnce(
                                    Result.failure(LocationClient.LocationException("Coordinates are not present."))
                                )

                                !isAcceptable(location) -> resumeOnce(
                                    Result.failure(LocationClient.LocationException("Coordinates are not updated!"))
                                )

                                else -> resumeOnce(
                                    Result.success(
                                        Coordinates(
                                            location.latitude,
                                            location.longitude
                                        )
                                    )
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

    private fun isAcceptable(location: Location): Boolean {
        val ageMs = Clock.System.now().toEpochMilliseconds() - location.time
        return ageMs < MAX_LOCATION_AGE_MS.inWholeMilliseconds && location.accuracy <= MAX_ACCURACY_METERS
    }

    private companion object {
        private val LOCATION_FETCH_TIMEOUT_MS = 8000.milliseconds
        private val MAX_LOCATION_AGE_MS = 2.minutes
        private const val MAX_ACCURACY_METERS = 200f
        private const val MAX_ATTEMPTS = 2
        private val RETRY_DELAY_MS = 500.milliseconds
        private val TRANSIENT_FAILURE_MESSAGES = setOf(
            "Coordinates are not present.",
            "Coordinates are not updated!",
        )
    }
}

private fun Context.hasLocationPermission(): Boolean =
    listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ).any { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
