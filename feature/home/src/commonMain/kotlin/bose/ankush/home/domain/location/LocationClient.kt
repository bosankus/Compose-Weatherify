package bose.ankush.home.domain.location

/** Platform-agnostic representation of a geographic coordinate. */
internal data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

internal interface LocationClient {
    suspend fun getCurrentLocation(): Result<Coordinates>

    fun hasLocationPermission(): Boolean

    class LocationException(
        message: String,
        val isGpsDisabled: Boolean = false,
    ) : Exception(message)
}
