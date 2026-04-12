package bose.ankush.weatherify.base.location

/**
 * Platform-agnostic representation of a geographic coordinate.
 * Used instead of android.location.Location to enable KMP compatibility.
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)
