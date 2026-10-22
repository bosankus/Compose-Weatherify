package bose.ankush.home.data.events

import bose.ankush.home.data.events.sync.PlaceEventSyncReport
import bose.ankush.home.data.events.sync.PlaceEventSyncScheduler
import bose.ankush.network.api.PlaceEventSyncApiService
import bose.ankush.network.model.CreatePlaceEventBody
import bose.ankush.network.model.PlaceEventApiResult
import bose.ankush.network.model.PlaceEventDto
import bose.ankush.network.model.PlaceEventSyncPage
import bose.ankush.network.model.UpdatePlaceEventBody
import bose.ankush.storage.api.PlaceEventStorage
import bose.ankush.storage.model.PlaceEventRecord
import bose.ankush.storage.model.PlaceEventSyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** In-memory [PlaceEventStorage] with Room's ordering and visibility rules. */
internal class FakePlaceEventStorage : PlaceEventStorage {
    val rows = MutableStateFlow<Map<String, PlaceEventRecord>>(emptyMap())
    var checkpoint: String? = null

    override fun observeEvents(): Flow<List<PlaceEventRecord>> =
        rows.map { all ->
            all.values
                .filter { it.deletedAt == null && it.syncState != PlaceEventSyncState.PENDING_DELETE }
                .sortedBy { it.startsAt }
        }

    override suspend fun getEvent(id: String): PlaceEventRecord? = rows.value[id]

    override suspend fun getPendingEvents(): List<PlaceEventRecord> =
        rows.value.values
            .filter { it.syncState != PlaceEventSyncState.SYNCED && it.syncState != PlaceEventSyncState.FAILED }
            .sortedBy { it.updatedAt }

    override suspend fun saveEvent(record: PlaceEventRecord) {
        rows.value = rows.value + (record.id to record)
    }

    override suspend fun replaceEventId(
        oldId: String,
        record: PlaceEventRecord,
    ) {
        rows.value = rows.value - oldId + (record.id to record)
    }

    override suspend fun deleteEvent(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun getSyncCheckpoint(): String? = checkpoint

    override suspend fun saveSyncCheckpoint(checkpoint: String) {
        this.checkpoint = checkpoint
    }

    override suspend fun clearSyncCheckpoint() {
        checkpoint = null
    }

    override suspend fun clearAll() {
        rows.value = emptyMap()
        checkpoint = null
    }

    fun row(id: String): PlaceEventRecord? = rows.value[id]
}

/** Scripted sync API. Each handler answers one call; [online] false makes every call throw. */
internal class FakePlaceEventSyncApi : PlaceEventSyncApiService {
    var online = true
    val calls = mutableListOf<String>()
    val created = mutableListOf<CreatePlaceEventBody>()
    val updated = mutableListOf<Pair<String, UpdatePlaceEventBody>>()

    var onCreate: suspend (CreatePlaceEventBody) -> PlaceEventApiResult<PlaceEventDto> = { body ->
        PlaceEventApiResult(201, body.toDto(updatedAt = T_SERVER))
    }
    var onUpdate: suspend (String, UpdatePlaceEventBody) -> PlaceEventApiResult<PlaceEventDto> = { id, body ->
        PlaceEventApiResult(200, body.toDto(id, T_SERVER))
    }
    var onDelete: suspend (String) -> PlaceEventApiResult<Unit> = { PlaceEventApiResult(204) }
    var onChanges: suspend (String?) -> PlaceEventApiResult<PlaceEventSyncPage> = {
        PlaceEventApiResult(200, PlaceEventSyncPage())
    }

    override suspend fun create(body: CreatePlaceEventBody): PlaceEventApiResult<PlaceEventDto> {
        record("POST ${body.id}")
        created += body
        return onCreate(body)
    }

    override suspend fun update(
        id: String,
        body: UpdatePlaceEventBody,
    ): PlaceEventApiResult<PlaceEventDto> {
        record("PUT $id")
        updated += id to body
        return onUpdate(id, body)
    }

    override suspend fun delete(id: String): PlaceEventApiResult<Unit> {
        record("DELETE $id")
        return onDelete(id)
    }

    override suspend fun changes(since: String?): PlaceEventApiResult<PlaceEventSyncPage> {
        record("GET since=$since")
        return onChanges(since)
    }

    private fun record(call: String) {
        if (!online) throw FakeOfflineException()
        calls += call
    }
}

internal class FakeOfflineException : Exception("offline")

/** Fixed time that moves forward by a second per read, and predictable client ids. */
internal class FakePlaceEventClock(
    start: Instant = Instant.parse("2026-10-22T10:00:00Z"),
) : PlaceEventClock {
    private var current = start
    private var nextId = 1

    override fun now(): Instant = current.also { current += 1.seconds }

    override fun newId(): String = "00000000-0000-4000-8000-${(nextId++).toString().padStart(12, '0')}"
}

internal class FakeSyncScheduler : PlaceEventSyncScheduler {
    var requests = 0
    var syncNowCalls = 0
    var report: PlaceEventSyncReport? = null

    override fun requestSync() {
        requests++
    }

    override suspend fun syncNow(): PlaceEventSyncReport? {
        syncNowCalls++
        return report
    }
}

internal const val T_SERVER = "2026-10-22T09:00:00.000Z"

internal fun CreatePlaceEventBody.toDto(updatedAt: String): PlaceEventDto =
    PlaceEventDto(id, placeName, lat, lon, title, startsAt, note, createdAt = updatedAt, updatedAt = updatedAt)

internal fun UpdatePlaceEventBody.toDto(
    id: String,
    updatedAt: String,
): PlaceEventDto =
    PlaceEventDto(id, placeName, lat, lon, title, startsAt, note, createdAt = T_SERVER, updatedAt = updatedAt)

internal fun serverDto(
    id: String,
    updatedAt: String = T_SERVER,
    title: String = "Server",
    deletedAt: String? = null,
): PlaceEventDto =
    PlaceEventDto(
        id = id,
        placeName = "Harbor",
        lat = 12.97,
        lon = 77.59,
        title = title,
        startsAt = "2026-10-24T14:00:00Z",
        createdAt = T_SERVER,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

internal fun localRecord(
    id: String,
    state: PlaceEventSyncState,
    updatedAt: String = "2026-10-22T10:00:00Z",
    serverUpdatedAt: String? = null,
    title: String = "Local",
): PlaceEventRecord =
    PlaceEventRecord(
        id = id,
        title = title,
        startsAt = "2026-10-24T14:00:00Z",
        note = null,
        placeName = "Harbor",
        lat = 12.97,
        lon = 77.59,
        createdAt = "2026-10-22T10:00:00Z",
        updatedAt = updatedAt,
        deletedAt = null,
        syncState = state,
        serverUpdatedAt = serverUpdatedAt,
    )
