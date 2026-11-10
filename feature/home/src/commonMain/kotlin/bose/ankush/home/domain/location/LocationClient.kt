package bose.ankush.home.domain.location

/** Platform-agnostic representation of a geographic coordinate. */
internal data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

internal interface LocationClient {
    suspend fun getCurrentLocation(): Result<Coordinates>

    fun hasLocationPermission(): Boolean

    /** [message] is for logs; what the user reads comes from [reason]. */
    class LocationException(
        message: String,
        val reason: Reason = Reason.NoFix,
    ) : Exception(message) {
        val isGpsDisabled: Boolean get() = reason == Reason.ServicesOff
    }

    enum class Reason {
        PermissionDenied,

        /** Location services (GPS) are switched off for the whole device. */
        ServicesOff,

        /** Allowed and switched on, but no usable fix arrived in time: indoors, simulator, etc. */
        NoFix,
    }
}
