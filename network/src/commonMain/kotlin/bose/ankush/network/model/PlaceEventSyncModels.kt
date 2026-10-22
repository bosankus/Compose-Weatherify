package bose.ankush.network.model

import kotlinx.serialization.Serializable

/**
 * A place event as the sync endpoints return it (POST, PUT, 409 conflict body and
 * GET /place-events/sync items). [id] is the client UUID, or the 24-hex ObjectId for rows created
 * before client ids existed. [note] is omitted when null; [deletedAt] is always present and set
 * only on a tombstone. Times are ISO-8601 instants; [updatedAt] has millisecond precision.
 */
@Serializable
data class PlaceEventDto(
    val id: String,
    val placeName: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val title: String = "",
    val startsAt: String = "",
    val note: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val deletedAt: String? = null,
)

/** POST /place-events with a client-generated lowercase UUID [id], which makes the create idempotent. */
@Serializable
data class CreatePlaceEventBody(
    val id: String,
    val placeName: String,
    val lat: Double,
    val lon: Double,
    val title: String,
    val startsAt: String,
    val note: String? = null,
)

/**
 * PUT /place-events/{id}: a full replace of the editable fields. [baseUpdatedAt] is the server
 * `updatedAt` this edit was made on; a newer server row answers 409 with the current copy.
 */
@Serializable
data class UpdatePlaceEventBody(
    val placeName: String,
    val lat: Double,
    val lon: Double,
    val title: String,
    val startsAt: String,
    val note: String? = null,
    val baseUpdatedAt: String? = null,
)

/**
 * One page of GET /place-events/sync?since=. Pass [nextSince] as `since` while [hasMore];
 * store [checkpoint] after the last page for the next sync.
 */
@Serializable
data class PlaceEventSyncPage(
    val items: List<PlaceEventDto> = emptyList(),
    val nextSince: String? = null,
    val checkpoint: String? = null,
    val hasMore: Boolean = false,
)

/**
 * A sync call's HTTP outcome. [statusCode] decides what the caller does (201/200/204/400/404/409/429);
 * [data] is the envelope's `data` when it parsed, and [retryAfterSeconds] is a numeric
 * `Retry-After` header, if any.
 */
data class PlaceEventApiResult<out T>(
    val statusCode: Int,
    val data: T? = null,
    val retryAfterSeconds: Long? = null,
)
