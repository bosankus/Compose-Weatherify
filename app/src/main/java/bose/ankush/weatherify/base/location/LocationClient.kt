package bose.ankush.weatherify.base.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {

    fun getLocationUpdates(interval: Long): Flow<Location>

    suspend fun getCurrentLocation(): Result<Location>

    fun hasLocationPermission(): Boolean

    class LocationException(message: String): Exception()
}
