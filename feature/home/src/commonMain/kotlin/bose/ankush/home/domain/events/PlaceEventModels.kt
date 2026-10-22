package bose.ankush.home.domain.events

import bose.ankush.home.domain.nearby.GeoPoint
import kotlin.time.Instant

/**
 * A home place event from the offline-first store. [id] is a lowercase client UUID, or the
 * 24-hex ObjectId of an event created before client ids existed. [isPendingSync] is true while a
 * local create, edit or delete has not reached the server yet.
 */
internal data class PlaceEventItem(
    val id: String,
    val title: String,
    val startsAt: Instant,
    val note: String?,
    val placeName: String,
    val point: GeoPoint,
    val isPendingSync: Boolean,
)

/** The editable fields of a place event, for a create or an edit. A blank [note] is stored as null. */
internal data class PlaceEventInput(
    val placeName: String,
    val point: GeoPoint,
    val title: String,
    val startsAt: Instant,
    val note: String? = null,
)

/** Events whose place lies within [radiusMeters] of [center]. */
internal data class EventArea(
    val center: GeoPoint,
    val radiusMeters: Double = PLACE_EVENT_RADIUS_METERS,
)

/** Same reach as the server's GET /place-events?lat=&lon= match (about 1 km). */
internal const val PLACE_EVENT_RADIUS_METERS = 1_000.0
