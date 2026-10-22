package bose.ankush.home.data.events.sync

import bose.ankush.home.data.events.parseInstant
import bose.ankush.home.data.events.toSyncedRecord
import bose.ankush.network.api.PlaceEventSyncApiService
import bose.ankush.network.model.PlaceEventApiResult
import bose.ankush.network.model.PlaceEventDto
import bose.ankush.network.model.PlaceEventSyncPage
import bose.ankush.storage.api.PlaceEventStorage
import bose.ankush.storage.model.PlaceEventRecord
import bose.ankush.storage.model.PlaceEventSyncState
import kotlin.time.Duration

/**
 * Pulls GET /place-events/sync from the stored checkpoint, following `nextSince` while
 * `hasMore`, and stores each page's checkpoint. After the last page it pulls once more from
 * (checkpoint - [overlap]) to catch rows the server stamped just behind the cursor; that pull
 * never moves the checkpoint.
 *
 * A 400 means the server no longer accepts the cursor: the checkpoint is dropped and the same run
 * pulls everything from the beginning, once. A 5xx or a network error stops the run (see
 * [SyncRun]) and keeps the checkpoint, so the retry resumes where this run stopped.
 *
 * Merging is idempotent, by id. A server row replaces a local one only when the local one is
 * synced or older (by `updatedAt`); a pending local edit is never clobbered by older data, a
 * pending local delete is only settled by a tombstone, a failed create/edit is left for the user,
 * and a failed delete takes the server copy.
 * A tombstone removes the local row.
 */
internal class PlaceEventPuller(
    private val storage: PlaceEventStorage,
    private val api: PlaceEventSyncApiService,
    private val overlap: Duration,
) {
    /** Returns how many server rows changed the local store. */
    suspend fun pull(run: SyncRun): Int {
        val cursor = PullCursor(storage.getSyncCheckpoint())
        while (cursor.fetching && !run.isStopped) {
            val result = run.call { api.changes(cursor.since) }
            cursor.applied += handle(run, cursor, result)
        }
        return cursor.applied + if (cursor.reachedEnd && !run.isStopped) overlapPull(run) else 0
    }

    /** Applies one answer to [cursor] and returns how many rows it changed. */
    private suspend fun handle(
        run: SyncRun,
        cursor: PullCursor,
        result: PlaceEventApiResult<PlaceEventSyncPage>?,
    ): Int {
        val page = result?.data?.takeIf { result.statusCode == HTTP_OK }
        return when {
            page != null -> applyPage(run, cursor, page)
            result?.statusCode == HTTP_BAD_REQUEST && cursor.canReset -> {
                run.write { storage.clearSyncCheckpoint() }
                cursor.resetToFullResync()
                0
            }
            else -> {
                cursor.fetching = false
                0
            }
        }
    }

    private suspend fun applyPage(
        run: SyncRun,
        cursor: PullCursor,
        page: PlaceEventSyncPage,
    ): Int {
        val applied = merge(run, page.items)
        page.checkpoint?.let { checkpoint -> run.write { storage.saveSyncCheckpoint(checkpoint) } }
        val next = page.nextSince
        if (!page.hasMore || next == null) {
            cursor.reachedEnd = true
            cursor.fetching = false
        } else {
            cursor.since = next
        }
        return applied
    }

    private suspend fun overlapPull(run: SyncRun): Int {
        val from =
            storage
                .getSyncCheckpoint()
                ?.let { parseInstant(it.substringBefore(CURSOR_SEPARATOR)) }
                ?.minus(overlap)
        val page =
            from?.let { start ->
                run.call { api.changes(start.toString()) }?.takeIf { it.statusCode == HTTP_OK }?.data
            }
        return page?.let { merge(run, it.items) } ?: 0
    }

    private suspend fun merge(
        run: SyncRun,
        items: List<PlaceEventDto>,
    ): Int {
        var applied = 0
        for (dto in items) {
            run.write { if (mergeOne(dto)) applied++ }
        }
        return applied
    }

    private suspend fun mergeOne(dto: PlaceEventDto): Boolean {
        val local = storage.getEvent(dto.id)
        val accept = local == null || serverWins(local, dto)
        when {
            !accept -> Unit
            dto.deletedAt != null -> local?.let { storage.deleteEvent(dto.id) }
            else -> storage.saveEvent(dto.toSyncedRecord())
        }
        return accept && (local != null || dto.deletedAt == null)
    }

    private fun serverWins(
        local: PlaceEventRecord,
        dto: PlaceEventDto,
    ): Boolean =
        when (local.syncState) {
            PlaceEventSyncState.SYNCED -> true
            PlaceEventSyncState.PENDING_UPSERT -> isOlder(local.updatedAt, dto.updatedAt)
            PlaceEventSyncState.PENDING_DELETE -> dto.deletedAt != null
            // A refused create/edit waits for the user (a discard resyncs to fetch the server
            // copy). A refused delete yields to whatever the server has: a tombstone removes it,
            // a live copy brings it back.
            PlaceEventSyncState.FAILED -> local.deletedAt != null
        }

    /** True when [local] is before [server]; an unreadable local time loses, an unreadable server time never wins. */
    private fun isOlder(
        local: String,
        server: String,
    ): Boolean {
        val serverTime = parseInstant(server)
        val localTime = parseInstant(local)
        return serverTime != null && (localTime == null || localTime < serverTime)
    }

    /** Where one pull stands. [canReset] allows a single full resync per run after a 400. */
    private class PullCursor(
        var since: String?,
    ) {
        var applied = 0
        var fetching = true
        var reachedEnd = false
        private var wasReset = false

        val canReset: Boolean get() = since != null && !wasReset

        fun resetToFullResync() {
            since = null
            wasReset = true
        }
    }

    private companion object {
        /** Server cursors are `<instant>` or `<instant>~<24-hex id>`. */
        const val CURSOR_SEPARATOR = '~'
    }
}
