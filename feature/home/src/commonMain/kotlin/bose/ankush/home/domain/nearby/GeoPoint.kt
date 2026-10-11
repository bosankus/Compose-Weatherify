package bose.ankush.home.domain.nearby

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A latitude/longitude pair in degrees. */
internal data class GeoPoint(
    val lat: Double,
    val lon: Double,
)

/** Great-circle (haversine) distance in metres. */
internal fun GeoPoint.distanceMetersTo(other: GeoPoint): Double {
    val startLat = lat.toRadians()
    val endLat = other.lat.toRadians()
    val dLat = (other.lat - lat).toRadians()
    val dLon = (other.lon - lon).toRadians()
    val a = sin(dLat / 2).pow(2) + cos(startLat) * cos(endLat) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

private fun Double.toRadians(): Double = this * PI / DEGREES_IN_HALF_TURN

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val DEGREES_IN_HALF_TURN = 180.0
