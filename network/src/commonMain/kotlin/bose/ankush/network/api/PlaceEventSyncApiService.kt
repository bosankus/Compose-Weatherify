package bose.ankush.network.api

import bose.ankush.network.model.CreatePlaceEventBody
import bose.ankush.network.model.PlaceEventApiResult
import bose.ankush.network.model.PlaceEventDto
import bose.ankush.network.model.PlaceEventSyncPage
import bose.ankush.network.model.UpdatePlaceEventBody

/**
 * Offline-sync surface of /place-events. Every call returns the HTTP status rather than throwing
 * on 4xx/5xx, because the sync engine branches on it. Transport failures (offline) still throw.
 * The JWT is attached by [bose.ankush.network.auth.interceptor.authorizedRequest].
 */
interface PlaceEventSyncApiService {
    /** POST /place-events: 201 created, 200 already exists (maybe a tombstone), 404 foreign id, 400 bad id. */
    suspend fun create(body: CreatePlaceEventBody): PlaceEventApiResult<PlaceEventDto>

    /** PUT /place-events/{id}: 200 updated, 409 conflict ([PlaceEventApiResult.data] is the server copy), 404 gone. */
    suspend fun update(
        id: String,
        body: UpdatePlaceEventBody,
    ): PlaceEventApiResult<PlaceEventDto>

    /** DELETE /place-events/{id}: 204 deleted, 404 unknown or already gone. */
    suspend fun delete(id: String): PlaceEventApiResult<Unit>

    /** GET /place-events/sync?since=: changes after [since] (null = from the beginning), tombstones included. */
    suspend fun changes(since: String?): PlaceEventApiResult<PlaceEventSyncPage>
}
