package bose.ankush.home.data.location

import bose.ankush.home.domain.location.HomeGeocoder
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLPlacemark
import kotlin.coroutines.resume

internal class IosHomeGeocoder : HomeGeocoder {
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): String? =
        suspendCancellableCoroutine { continuation ->
            val geocoder = CLGeocoder()
            val location = CLLocation(latitude, longitude)
            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                @Suppress("UNCHECKED_CAST")
                val placemark = (placemarks as? List<CLPlacemark>)?.firstOrNull()
                val result =
                    if (error != null || placemark == null) {
                        null
                    } else {
                        val locality = placemark.locality
                        val country = placemark.country
                        when {
                            locality != null -> "$locality, $country"
                            country != null -> country
                            else -> null
                        }
                    }
                if (continuation.isActive) continuation.resume(result)
            }
        }
}
