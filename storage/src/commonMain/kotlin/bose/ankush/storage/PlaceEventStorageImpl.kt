package bose.ankush.storage

import bose.ankush.storage.api.PlaceEventStorage
import bose.ankush.storage.common.PLACE_EVENTS_SYNC_STREAM
import bose.ankush.storage.model.PlaceEventRecord
import bose.ankush.storage.model.PlaceEventSyncState
import bose.ankush.storage.room.PlaceEventDao
import bose.ankush.storage.room.PlaceEventEntity
import bose.ankush.storage.room.SyncCheckpointDao
import bose.ankush.storage.room.SyncCheckpointEntity
import bose.ankush.storage.room.WeatherDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed [PlaceEventStorage], shared by Android and iOS. */
class PlaceEventStorageImpl(
    database: WeatherDatabase,
) : PlaceEventStorage {
    private val dao: PlaceEventDao = database.placeEventDao()
    private val checkpoints: SyncCheckpointDao = database.syncCheckpointDao()

    override fun observeEvents(): Flow<List<PlaceEventRecord>> =
        dao.observeVisible().map { rows -> rows.map { it.toRecord() } }

    override suspend fun getEvent(id: String): PlaceEventRecord? = dao.getById(id)?.toRecord()

    override suspend fun getPendingEvents(): List<PlaceEventRecord> = dao.getPending().map { it.toRecord() }

    override suspend fun saveEvent(record: PlaceEventRecord) = dao.upsert(record.toEntity())

    override suspend fun replaceEventId(
        oldId: String,
        record: PlaceEventRecord,
    ) = dao.rekey(oldId, record.toEntity())

    override suspend fun deleteEvent(id: String) = dao.deleteById(id)

    override suspend fun getSyncCheckpoint(): String? = checkpoints.get(PLACE_EVENTS_SYNC_STREAM)

    override suspend fun saveSyncCheckpoint(checkpoint: String) =
        checkpoints.put(SyncCheckpointEntity(PLACE_EVENTS_SYNC_STREAM, checkpoint))

    override suspend fun clearAll() = dao.clearAll()
}

internal fun PlaceEventEntity.toRecord(): PlaceEventRecord =
    PlaceEventRecord(
        id = id,
        title = title,
        startsAt = startsAt,
        note = note,
        placeName = placeName,
        lat = lat,
        lon = lon,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        // An unknown name (e.g. after a downgrade) is safest as pending: it gets pushed, not lost.
        syncState =
            PlaceEventSyncState.entries.firstOrNull { it.name == syncState }
                ?: PlaceEventSyncState.PENDING_UPSERT,
        serverUpdatedAt = serverUpdatedAt,
    )

internal fun PlaceEventRecord.toEntity(): PlaceEventEntity =
    PlaceEventEntity(
        id = id,
        title = title,
        startsAt = startsAt,
        note = note,
        placeName = placeName,
        lat = lat,
        lon = lon,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncState = syncState.name,
        serverUpdatedAt = serverUpdatedAt,
    )
