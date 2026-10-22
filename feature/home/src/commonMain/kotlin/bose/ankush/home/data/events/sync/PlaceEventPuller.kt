package bose.ankush.home.data.events.sync

import bose.ankush.home.data.events.parseInstant
import bose.ankush.home.data.events.toSyncedRecord
import bose.ankush.network.api.PlaceEventSyncApiService
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
 * Merging is idempotent, by id. A server row replaces a local one only when the local one is
 * synced or older (by `updatedAt`); a pending local edit is never clobbered by older data, and a
 * pending local delete is only settled by a tombstone. A tombstone removes the local row.
 */
internal class PlaceEventPuller(
    private val storage: PlaceEventStorage,
    private val api: PlaceEventSyncApiService,
    private val overlap: Duration,
) {
    /** Returns how many server rows changed the local store. */
    suspend fun pull(run: SyncRun): Int {
        var since = storage.getSyncCheckpoint()
        var applied = 0
        var reachedEnd = false
        while (!reachedEnd) {
            val page = fetch(run, since) ?: break
            applied += merge(run, page.items)
            page.checkpoint?.let { checkpoint -> run.write { storage.saveSyncCheckpoint(checkpoint) } }
            val next = page.nextSince
            reachedEnd = !page.hasMore || next == null || run.isStopped
            since = next ?: since
        }
        return applied + if (reachedEnd && !run.isStopped) overlapPull(run) else 0
    }

    private suspend fun overlapPull(run: SyncRun): Int {
        val from =
            storage
                .getSyncCheckpoint()
                ?.let { parseInstant(it.substringBefore(CURSOR_SEPARATOR)) }
                ?.minus(overlap)
        val page = from?.let { fetch(run, it.toString()) }
        return page?.let { merge(run, it.items) } ?: 0
    }

    private suspend fun fetch(
        run: SyncRun,
        since: String?,
    ): PlaceEventSyncPage? =
        run
            .call { api.changes(since) }
            ?.takeIf { it.statusCode == HTTP_OK }
            ?.data

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

    private companion object {
        /** Server cursors are `<instant>` or `<instant>~<24-hex id>`. */
        const val CURSOR_SEPARATOR = '~'
    }
}
