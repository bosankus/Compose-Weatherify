package bose.ankush.storage.api

import bose.ankush.storage.model.PlaceEventRecord
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first store for home place events and their sync checkpoint.
 *
 * Callers write here first and let the sync engine reconcile with the server, so every write
 * is local and immediate.
 */
interface PlaceEventStorage {
    /** Every event a user should see: not deleted and not waiting for a delete. */
    fun observeEvents(): Flow<List<PlaceEventRecord>>

    suspend fun getEvent(id: String): PlaceEventRecord?

    /** Rows waiting to be pushed (upserts and deletes), oldest local change first. */
    suspend fun getPendingEvents(): List<PlaceEventRecord>

    /** Inserts or replaces the row with [PlaceEventRecord.id]. */
    suspend fun saveEvent(record: PlaceEventRecord)

    /** Atomically removes [oldId] and stores [record] under its new id. */
    suspend fun replaceEventId(
        oldId: String,
        record: PlaceEventRecord,
    )

    suspend fun deleteEvent(id: String)

    /** The last `checkpoint` from GET /place-events/sync, or null before the first sync. */
    suspend fun getSyncCheckpoint(): String?

    suspend fun saveSyncCheckpoint(checkpoint: String)

    /** Removes every event and the checkpoint (sign-out). */
    suspend fun clearAll()
}
