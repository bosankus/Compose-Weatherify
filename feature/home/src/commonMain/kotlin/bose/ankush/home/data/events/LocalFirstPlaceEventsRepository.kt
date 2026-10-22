package bose.ankush.home.data.events

import bose.ankush.home.data.events.sync.PlaceEventSyncScheduler
import bose.ankush.home.domain.events.EventArea
import bose.ankush.home.domain.events.PlaceEventInput
import bose.ankush.home.domain.events.PlaceEventItem
import bose.ankush.home.domain.nearby.distanceMetersTo
import bose.ankush.home.domain.repository.PlaceEventsRepository
import bose.ankush.storage.api.PlaceEventStorage
import bose.ankush.storage.model.PlaceEventRecord
import bose.ankush.storage.model.PlaceEventSyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [PlaceEventsRepository] over Room. Reads filter the stored events by distance on the client;
 * writes go to Room first, marked pending, then ask [sync] to push them.
 */
internal class LocalFirstPlaceEventsRepository(
    private val storage: PlaceEventStorage,
    private val lock: PlaceEventWriteLock,
    private val sync: PlaceEventSyncScheduler,
    private val clock: PlaceEventClock = PlaceEventClock.System,
) : PlaceEventsRepository {
    override fun observeEvents(area: EventArea): Flow<List<PlaceEventItem>> =
        storage.observeEvents().map { rows ->
            rows
                .mapNotNull { it.toItem() }
                .filter { it.point.distanceMetersTo(area.center) <= area.radiusMeters }
                .sortedBy { it.startsAt }
        }

    override suspend fun createEvent(input: PlaceEventInput): Result<PlaceEventItem> =
        write {
            val now = clock.now().toString()
            val record =
                PlaceEventRecord(
                    id = clock.newId(),
                    title = "",
                    startsAt = "",
                    note = null,
                    placeName = "",
                    lat = 0.0,
                    lon = 0.0,
                    createdAt = now,
                    updatedAt = now,
                    syncState = PlaceEventSyncState.PENDING_UPSERT,
                ).withInput(input)
            lock.withLock { storage.saveEvent(record) }
            record
        }.mapCatching { it.toItemOrThrow() }

    override suspend fun updateEvent(
        id: String,
        input: PlaceEventInput,
    ): Result<PlaceEventItem> =
        write {
            lock.withLock {
                val existing = storage.getEvent(id)?.takeIf { it.syncState != PlaceEventSyncState.PENDING_DELETE }
                val updated =
                    requireNotNull(existing) { "No event $id to edit." }
                        .withInput(input)
                        .copy(updatedAt = clock.now().toString(), syncState = PlaceEventSyncState.PENDING_UPSERT)
                storage.saveEvent(updated)
                updated
            }
        }.mapCatching { it.toItemOrThrow() }

    override suspend fun deleteEvent(id: String): Result<Unit> =
        write {
            lock.withLock {
                storage.getEvent(id)?.let { existing ->
                    val now = clock.now().toString()
                    // Kept as a pending delete even if it never reached the server: a POST whose
                    // response was lost may have created it there. DELETE's 404 settles that case.
                    storage.saveEvent(
                        existing.copy(
                            updatedAt = now,
                            deletedAt = now,
                            syncState = PlaceEventSyncState.PENDING_DELETE,
                        ),
                    )
                }
            }
        }

    override suspend fun hasSynced(): Boolean = storage.getSyncCheckpoint() != null

    override suspend fun clearAll() = lock.clear { storage.clearAll() }

    /** Runs a local write, then asks for a sync. Storage errors become a failed [Result]. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> write(block: suspend () -> T): Result<T> =
        try {
            val saved = block()
            sync.requestSync()
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}

private fun PlaceEventRecord.toItemOrThrow(): PlaceEventItem =
    checkNotNull(toItem()) { "Stored event $id has an unreadable start time." }
