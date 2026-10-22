package bose.ankush.home.domain.events

import bose.ankush.home.domain.nearby.GeoPoint
import kotlin.time.Instant

/**
 * A home place event from the offline-first store. [id] is a lowercase client UUID, or the
 * 24-hex ObjectId of an event created before client ids existed. [syncStatus] says whether the
 * server has it; [syncError] is the server's reason when it is [PlaceEventSyncStatus.FAILED]
 * ("couldn't sync"), null otherwise.
 */
internal data class PlaceEventItem(
    val id: String,
    val title: String,
    val startsAt: Instant,
    val note: String?,
    val placeName: String,
    val point: GeoPoint,
    val syncStatus: PlaceEventSyncStatus,
    val syncError: String? = null,
) {
    /** True while a local create, edit or delete has not reached the server yet. */
    val isPendingSync: Boolean get() = syncStatus == PlaceEventSyncStatus.PENDING
}

/** Where a place event stands against the server, as the UI shows it. */
internal enum class PlaceEventSyncStatus {
    SYNCED,

    /** A local change is waiting to be sent; it is retried by itself. */
    PENDING,

    /** The server refused the change. Not retried until the user edits it or discards it. */
    FAILED,
}

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
