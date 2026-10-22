package bose.ankush.home.data.events.sync

import bose.ankush.home.data.events.hasSameContentAs
import bose.ankush.home.data.events.toCreateBody
import bose.ankush.home.data.events.toPendingDelete
import bose.ankush.home.data.events.toSyncedRecord
import bose.ankush.home.data.events.toUpdateBody
import bose.ankush.network.api.PlaceEventSyncApiService
import bose.ankush.network.model.PlaceEventDto
import bose.ankush.storage.api.PlaceEventStorage
import bose.ankush.storage.model.PlaceEventRecord
import bose.ankush.storage.model.PlaceEventSyncState

/**
 * Pushes pending rows one request at a time, oldest local change first:
 * - never accepted by the server ([PlaceEventRecord.serverUpdatedAt] null): POST with the client id;
 * - accepted before: PUT with `baseUpdatedAt`;
 * - deleted locally: DELETE.
 *
 * A row the user changed while its request was in flight keeps its newer local edit and stays
 * pending, rebased on the server's `updatedAt`, so the next run sends it.
 *
 * A 400 on POST or PUT marks the row [PlaceEventSyncState.FAILED] with the server's message. It
 * stays visible and is not sent again until the user edits it (pending again) or discards it.
 * A refused DELETE (a 4xx other than 404) is marked failed too, but stays hidden.
 */
internal class PlaceEventPusher(
    private val storage: PlaceEventStorage,
    private val api: PlaceEventSyncApiService,
    private val newId: () -> String,
) {
    /** Returns how many pending rows were settled with the server. */
    suspend fun push(run: SyncRun): Int {
        var settled = 0
        for (row in storage.getPendingEvents()) {
            if (run.isStopped) break
            if (pushOne(row, run)) settled++
        }
        return settled
    }

    private suspend fun pushOne(
        row: PlaceEventRecord,
        run: SyncRun,
    ): Boolean =
        when {
            row.syncState == PlaceEventSyncState.PENDING_DELETE -> pushDelete(row, run)
            row.serverUpdatedAt == null -> pushCreate(row, run, mayRekey = true)
            else -> pushUpdate(row, run)
        }

    /**
     * 204 deleted and 404 already gone both settle the delete: the row is removed. Any other 4xx
     * (401/403/408/429 never get here, see [SyncRun.call]) marks it failed: still hidden as a
     * delete, not sent again, and reconciled by a later pull. 5xx and offline stay pending.
     */
    private suspend fun pushDelete(
        row: PlaceEventRecord,
        run: SyncRun,
    ): Boolean {
        val result = run.call { api.delete(row.id) }
        val status = result?.statusCode
        return when {
            status == HTTP_NO_CONTENT || status == HTTP_OK || status == HTTP_NOT_FOUND -> {
                run.write { storage.deleteEvent(row.id) }
                true
            }
            status != null && status in HTTP_CLIENT_ERRORS ->
                markFailed(run, row, result?.message ?: REJECTED_DELETE_MESSAGE)
            else -> false
        }
    }

    /**
     * 201 new / 200 existing: take the server copy (a tombstone removes the row). 404: the id
     * belongs to another account, so the row gets a fresh UUID and is posted once more. 400: the
     * server refused it, so it is marked failed. Anything else leaves it pending.
     */
    private suspend fun pushCreate(
        row: PlaceEventRecord,
        run: SyncRun,
        mayRekey: Boolean,
    ): Boolean {
        val result = run.call { api.create(row.toCreateBody()) }
        val dto = result?.data
        return when {
            result == null -> false
            (result.statusCode == HTTP_CREATED || result.statusCode == HTTP_OK) && dto != null -> {
                adopt(run, row, dto, keepLocalContent = result.statusCode == HTTP_OK)
                true
            }
            result.statusCode == HTTP_NOT_FOUND && mayRekey ->
                rekey(row, run)?.let { pushCreate(it, run, mayRekey = false) } ?: false
            result.statusCode == HTTP_BAD_REQUEST -> markFailed(run, row, result.message)
            else -> false
        }
    }

    /**
     * 200: take the server copy. 409: `data` is the current server copy, and it wins. 404: deleted
     * or unknown there, so drop it. 400: the server refused the edit, so it is marked failed.
     */
    private suspend fun pushUpdate(
        row: PlaceEventRecord,
        run: SyncRun,
    ): Boolean {
        val result = run.call { api.update(row.id, row.toUpdateBody()) }
        val dto = result?.data
        return when {
            result == null -> false
            result.statusCode == HTTP_OK && dto != null -> {
                adopt(run, row, dto, keepLocalContent = false)
                true
            }
            result.statusCode == HTTP_CONFLICT && dto != null -> {
                run.write { storeServerCopy(dto) }
                true
            }
            result.statusCode == HTTP_NOT_FOUND -> {
                run.write { storage.deleteEvent(row.id) }
                true
            }
            result.statusCode == HTTP_BAD_REQUEST -> markFailed(run, row, result.message)
            else -> false
        }
    }

    /**
     * Marks [pushed] failed with the server's [message]; returns false (nothing settled). A newer
     * local edit made while the request was in flight is left pending, so it gets its own try.
     */
    private suspend fun markFailed(
        run: SyncRun,
        pushed: PlaceEventRecord,
        message: String?,
    ): Boolean {
        run.write {
            if (storage.getEvent(pushed.id) == pushed) {
                // A refused delete must stay hidden, so it always carries a deletedAt.
                val deletedAt =
                    if (pushed.syncState == PlaceEventSyncState.PENDING_DELETE) {
                        pushed.deletedAt ?: pushed.updatedAt
                    } else {
                        pushed.deletedAt
                    }
                storage.saveEvent(
                    pushed.copy(
                        syncState = PlaceEventSyncState.FAILED,
                        syncError = message ?: REJECTED_MESSAGE,
                        deletedAt = deletedAt,
                    ),
                )
            }
        }
        return false
    }

    /**
     * Records the server's answer to [pushed]. If the row changed locally meanwhile, or (for a
     * 200 on POST) the server holds older content than ours, the row stays pending on the new
     * base instead of losing the local edit. If it was deleted locally meanwhile (a create that
     * had not been acknowledged is deleted with no request), the server copy is queued for DELETE.
     */
    private suspend fun adopt(
        run: SyncRun,
        pushed: PlaceEventRecord,
        dto: PlaceEventDto,
        keepLocalContent: Boolean,
    ) = run.write {
        val current = storage.getEvent(pushed.id)
        // Any local change since the request was built (fields, state, timestamps) counts as touched.
        val untouched = current == pushed
        val serverHasOurContent = !keepLocalContent || current?.hasSameContentAs(dto) == true
        when {
            dto.deletedAt != null -> storage.deleteEvent(pushed.id)
            current == null -> storage.saveEvent(dto.toPendingDelete())
            untouched && serverHasOurContent -> storage.saveEvent(dto.toSyncedRecord())
            else -> storage.saveEvent(current.copy(serverUpdatedAt = dto.updatedAt))
        }
    }

    private suspend fun storeServerCopy(dto: PlaceEventDto) {
        if (dto.deletedAt != null) storage.deleteEvent(dto.id) else storage.saveEvent(dto.toSyncedRecord())
    }

    /** Moves the current version of [row] to a fresh client id; null if it is gone. */
    private suspend fun rekey(
        row: PlaceEventRecord,
        run: SyncRun,
    ): PlaceEventRecord? {
        var rekeyed: PlaceEventRecord? = null
        run.write {
            storage.getEvent(row.id)?.let { current ->
                val moved = current.copy(id = newId())
                storage.replaceEventId(row.id, moved)
                rekeyed = moved
            }
        }
        return rekeyed
    }
}

/** Shown when a 400 came without a message. */
internal const val REJECTED_MESSAGE = "The server couldn't accept this event."

/** Kept when a refused DELETE came without a message. */
internal const val REJECTED_DELETE_MESSAGE = "The server couldn't delete this event."
