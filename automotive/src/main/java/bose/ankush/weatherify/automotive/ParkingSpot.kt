package bose.ankush.weatherify.automotive

/**
 * One parking lot shown in the car UI.
 *
 * OpenStreetMap has no live "free slots" data, so we show what it does have:
 * the total [capacity] and whether the lot charges a fee. Both are optional
 * because many OSM parking lots don't have those tags.
 */
data class ParkingSpot(
    val name: String,
    val distanceKm: Double,
    val capacity: Int?,
    val isPaid: Boolean?,
    val lat: Double,
    val lng: Double,
)
