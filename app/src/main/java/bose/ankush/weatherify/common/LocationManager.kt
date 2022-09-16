package bose.ankush.weatherify.common

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import bose.ankush.weatherify.common.Extension.openAppSystemSettings
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import javax.inject.Inject

class LocationManager @Inject constructor(
    private val fusedLocation: FusedLocationProviderClient
) {

    @SuppressLint("MissingPermission")
    @Composable
    @ExperimentalPermissionsApi
    fun getLatLang(context: Context): Coordinates {
        val coordinates = Coordinates(0.0, 0.0)

        /**
         * Checking and requesting permission. If granted it will fetch current lat lang,
         * else it will request for permission.
         * If denied, will show popup to open app settings and grant location permission.
         */
        LocationPermissionManager.RequestPermission(
            actionPermissionGranted = {
                fusedLocation.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            coordinates.lat = location.latitude
                            coordinates.long = location.longitude
                        }
                    }
            },
            actionPermissionDenied = { context.openAppSystemSettings() }
        )

        return coordinates
    }
}


data class Coordinates(var lat: Double, var long: Double)