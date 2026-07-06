package bose.ankush.home.domain.location

import kotlinx.coroutines.flow.Flow

/** Platform-agnostic representation of a geographic coordinate. */
internal data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

internal object LocationPermissions {
    const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
}

internal interface LocationClient {
    fun getLocationUpdates(interval: Long): Flow<Coordinates>

    suspend fun getCurrentLocation(): Result<Coordinates>

    fun hasLocationPermission(): Boolean

    class LocationException(
        message: String,
    ) : Exception(message)
}
