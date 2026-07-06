package bose.ankush.home.data.location

import android.content.Context
import android.location.Geocoder
import bose.ankush.home.domain.location.HomeGeocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

internal class AndroidHomeGeocoder(
    private val context: Context,
) : HomeGeocoder {
    @Suppress("DEPRECATION")
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                val cityName = address?.locality ?: address?.subAdminArea
                val countryName = address?.countryName
                when {
                    cityName != null -> "$cityName, $countryName"
                    countryName != null -> countryName
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
}
