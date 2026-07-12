package bose.ankush.home.presentation.util

internal sealed interface CoordinateResolution {
    data class Ready(
        val lat: Double,
        val lon: Double,
        val isOverridden: Boolean,
        val overrideName: String?,
    ) : CoordinateResolution

    data class LocationError(
        val message: String,
        val isGpsDisabled: Boolean,
        val isPermissionDenied: Boolean = false,
        // A fresh GPS fix can fail transiently (cold start, indoors, brief permission race).
        // Rather than blocking the whole screen, fall back to the last known (non-override)
        // coordinates so weather still loads, and surface the failure as a dismissible banner.
        val fallback: Ready?,
    ) : CoordinateResolution

    data object NoCoordinates : CoordinateResolution
}
