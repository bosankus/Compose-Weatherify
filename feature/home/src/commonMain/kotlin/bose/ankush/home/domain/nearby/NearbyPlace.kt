package bose.ankush.home.domain.nearby

/** The active place a new event is pinned to. */
internal data class NearbyPlace(
    val name: String,
    val point: GeoPoint,
)
