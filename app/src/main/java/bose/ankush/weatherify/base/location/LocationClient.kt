package bose.ankush.weatherify.base.location

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic location client interface.
 * Uses [Coordinates] instead of android.location.Location to enable KMP compatibility.
 */
interface LocationClient {
    fun getLocationUpdates(interval: Long): Flow<Coordinates>

    suspend fun getCurrentLocation(): Result<Coordinates>

    fun hasLocationPermission(): Boolean

    class LocationException(
        message: String,
    ) : Exception(message)
}
