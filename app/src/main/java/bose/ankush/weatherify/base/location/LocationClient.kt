package bose.ankush.weatherify.base.location

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.Flow

interface LocationClient {

    suspend fun getCurrentLocation(): Pair<Double, Double>?

    fun getLocationUpdates(interval: Long): Flow<Location>

    class LocationException(message: String): Exception()
}