package bose.ankush.network.model

import kotlinx.serialization.Serializable

/** One row from GET /place-events. */
@Serializable
data class PlaceEvent(
    val id: String = "",
    val placeName: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val title: String = "",
    val startsAt: String = "",
    val note: String? = null,
    val createdAt: String = "",
)

/**
 * POST /place-events. [note] is sent as null. No account id.
 * [startsAt] is an ISO-8601 instant supplied by the user.
 */
@Serializable
data class CreatePlaceEventRequest(
    val placeName: String,
    val lat: Double,
    val lon: Double,
    val title: String,
    val startsAt: String,
    val note: String? = null,
)
