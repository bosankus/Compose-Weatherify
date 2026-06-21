package bose.ankush.weatherify.base.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import bose.ankush.weatherify.base.common.Extension.hasLocationPermission
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class DeviceLocationClient
@Inject
constructor(
    private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationClient {
    private fun checkLocationPermission() {
        if (!context.hasLocationPermission()) {
            throw LocationClient.LocationException("Location permission is not given.")
        }
    }

    private fun checkGpsEnabled(): Pair<Boolean, Boolean> {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled =
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isGPSEnabled && !isNetworkEnabled) {
            throw LocationClient.LocationException("GPS is disabled")
        }
        return Pair(isGPSEnabled, isNetworkEnabled)
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Coordinates> =
        callbackFlow {
            checkLocationPermission()
            checkGpsEnabled()

            val request =
                LocationRequest
                    .Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        interval,
                    ).apply {
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

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper(),
            )

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
                        val coords = Coordinates(location.latitude, location.longitude)
                        continuation.resume(Result.success(coords))
                    } else {
                        val ex = LocationClient.LocationException("Location is null")
                        continuation.resume(Result.failure(ex))
                    }
                }.addOnFailureListener { e ->
                    val ex = LocationClient.LocationException(e.message ?: "Unknown error")
                    continuation.resume(Result.failure(ex))
                }

            continuation.invokeOnCancellation { cts.cancel() }
        }

    override fun hasLocationPermission(): Boolean = context.hasLocationPermission()
}
