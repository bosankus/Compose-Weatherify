package bose.ankush.storage.model

/** Where a locally stored place event stands against the server. */
enum class PlaceEventSyncState {
    /** Matches the server copy as of [PlaceEventRecord.serverUpdatedAt]. */
    SYNCED,

    /** Created or edited on this device; not yet accepted by the server. */
    PENDING_UPSERT,

    /** Deleted on this device; hidden locally until the server confirms the delete. */
    PENDING_DELETE,

    /**
     * The server refused the create or edit (400). Kept and shown with [PlaceEventRecord.syncError],
     * never retried by itself: an edit queues it again, or the user discards it.
     */
    FAILED,
}

/**
 * One place event in the offline-first store.
 *
 * [id] is a lowercase client UUID for events made on a device, or the 24-hex ObjectId of an
 * older server row. Times are ISO-8601 instants. [updatedAt] is the last change on whichever side
 * wrote the row; [serverUpdatedAt] is the server's `updatedAt` the row was last reconciled with,
 * sent as PUT's `baseUpdatedAt`, and null until the server has accepted the event. [syncError]
 * is the server's message for a [PlaceEventSyncState.FAILED] row, null otherwise.
 */
data class PlaceEventRecord(
    val id: String,
    val title: String,
    val startsAt: String,
    val note: String?,
    val placeName: String,
    val lat: Double,
    val lon: Double,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val syncState: PlaceEventSyncState = PlaceEventSyncState.SYNCED,
    val serverUpdatedAt: String? = null,
    val syncError: String? = null,
)
