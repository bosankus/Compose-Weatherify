package bose.ankush.home.data.events.sync

import bose.ankush.home.data.events.PlaceEventClock
import bose.ankush.home.data.events.PlaceEventWriteLock
import bose.ankush.network.api.PlaceEventSyncApiService
import bose.ankush.storage.api.PlaceEventStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Shared offline-first sync for home place events: push pending rows one at a time, then pull
 * changes since the stored checkpoint (plus a short overlap pull) and store the new checkpoint.
 *
 * One run at a time. A run sends at most [PlaceEventSyncConfig.maxRequestsPerRun] requests and
 * stops cleanly on 429, offline, 5xx or 401, leaving unsent work pending; [PlaceEventSyncReport.retryAfter]
 * tells the scheduler when to try again (Retry-After when the server sent one, else exponential).
 */
internal class PlaceEventSyncEngine(
    storage: PlaceEventStorage,
    api: PlaceEventSyncApiService,
    private val lock: PlaceEventWriteLock,
    private val config: PlaceEventSyncConfig = PlaceEventSyncConfig(),
    clock: PlaceEventClock = PlaceEventClock.System,
) {
    private val pusher = PlaceEventPusher(storage, api, clock::newId)
    private val puller = PlaceEventPuller(storage, api, config.overlap)
    private val runMutex = Mutex()
    private var consecutiveFailures = 0

    suspend fun sync(): PlaceEventSyncReport =
        runMutex.withLock {
            val run = SyncRun(config.maxRequestsPerRun, lock, lock.currentGeneration())
            val pushed = pusher.push(run)
            val pulled = if (run.isStopped) 0 else puller.pull(run)
            report(run, pushed, pulled)
        }

    private fun report(
        run: SyncRun,
        pushed: Int,
        pulled: Int,
    ): PlaceEventSyncReport {
        val outcome = run.stopReason ?: PlaceEventSyncOutcome.COMPLETED
        val failed = outcome in RETRIED_FAILURES
        consecutiveFailures = if (failed) consecutiveFailures + 1 else 0
        val retryAfter =
            when {
                outcome == PlaceEventSyncOutcome.REQUEST_CAP -> config.capCooldown
                !failed || consecutiveFailures > config.maxAutoRetries -> null
                outcome == PlaceEventSyncOutcome.RATE_LIMITED && run.retryAfterSeconds != null ->
                    run.retryAfterSeconds?.seconds
                else -> backoff(consecutiveFailures)
            }
        val serverRetryAfter = run.retryAfterSeconds?.seconds?.takeIf { outcome == PlaceEventSyncOutcome.RATE_LIMITED }
        return PlaceEventSyncReport(outcome, run.requests, pushed, pulled, retryAfter, serverRetryAfter)
    }

    /** [PlaceEventSyncConfig.baseBackoff] doubled per consecutive failure, capped. */
    private fun backoff(failures: Int): Duration {
        val doublings = (failures - 1).coerceIn(0, MAX_DOUBLINGS)
        return (config.baseBackoff * (1 shl doublings).toDouble()).coerceAtMost(config.maxBackoff)
    }

    private companion object {
        const val MAX_DOUBLINGS = 16
        val RETRIED_FAILURES =
            setOf(
                PlaceEventSyncOutcome.RATE_LIMITED,
                PlaceEventSyncOutcome.OFFLINE,
                PlaceEventSyncOutcome.SERVER_ERROR,
            )
    }
}
