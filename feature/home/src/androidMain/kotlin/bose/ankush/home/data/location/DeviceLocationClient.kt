package bose.ankush.home.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import bose.ankush.home.domain.location.Coordinates
import bose.ankush.home.domain.location.LocationClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class DeviceLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationClient {
    private fun checkLocationPermission() {
        if (!context.hasLocationPermission()) {
            throw LocationClient.LocationException("Location permission is not given.")
        }
    }

    private fun checkGpsEnabled() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isGPSEnabled && !isNetworkEnabled) {
            throw LocationClient.LocationException("GPS is disabled")
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Coordinates> =
        callbackFlow {
            checkLocationPermission()
            checkGpsEnabled()

            val request =
                LocationRequest
                    .Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                    .apply {
                        setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                        setWaitForAccurateLocation(true)
                    }.build()

            val locationCallback =
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        super.onLocationResult(result)
                        result.locations.lastOrNull()?.let { location ->
                            launch { send(location) }
                        }
                    }
                }

            client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

            awaitClose { client.removeLocationUpdates(locationCallback) }
        }.map { loc -> Coordinates(loc.latitude, loc.longitude) }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Coordinates> =
        suspendCancellableCoroutine { continuation ->
            try {
                checkLocationPermission()
                checkGpsEnabled()
            } catch (e: LocationClient.LocationException) {
                continuation.resume(Result.failure(e))
                return@suspendCancellableCoroutine
            }

            val cts = CancellationTokenSource()

            client
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(Result.success(Coordinates(location.latitude, location.longitude)))
                    } else {
                        continuation.resume(Result.failure(LocationClient.LocationException("Location is not present")))
                    }
                }.addOnFailureListener { e ->
                    continuation.resume(Result.failure(LocationClient.LocationException(e.message ?: "Unknown error")))
                }

            continuation.invokeOnCancellation { cts.cancel() }
        }

    override fun hasLocationPermission(): Boolean = context.hasLocationPermission()
}

private fun Context.hasLocationPermission(): Boolean =
    listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ).all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
