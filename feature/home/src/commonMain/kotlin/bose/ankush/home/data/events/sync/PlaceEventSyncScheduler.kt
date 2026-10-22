package bose.ankush.home.data.events.sync

import bose.ankush.home.PlaceEventSyncTrigger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** Runs place-event syncs: on request in the background, or now for a caller that must wait. */
internal interface PlaceEventSyncScheduler : PlaceEventSyncTrigger {
    /** Syncs now and returns the report; null while signed out. */
    suspend fun syncNow(): PlaceEventSyncReport?
}

/**
 * In-process scheduler used on both platforms. Requests are conflated into one background run at
 * a time, and a failed or capped run re-requests itself after [PlaceEventSyncReport.retryAfter].
 *
 * Android has no WorkManager dependency yet, so syncs run only while the process is alive
 * (create/edit/delete, app start, the first events load); a WorkManager unique job can replace
 * this binding once the dependency is approved. iOS triggers on foreground only, by design.
 */
internal class InProcessPlaceEventSyncScheduler(
    private val engine: PlaceEventSyncEngine,
    private val scope: CoroutineScope,
    private val isSignedIn: suspend () -> Boolean,
) : PlaceEventSyncScheduler {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private var retryJob: Job? = null

    init {
        scope.launch {
            requests.consumeEach { runQuietly() }
        }
    }

    override fun requestSync() {
        requests.trySend(Unit)
    }

    override suspend fun syncNow(): PlaceEventSyncReport? {
        val report = if (isSignedIn()) engine.sync() else null
        report?.retryAfter?.let(::scheduleRetry)
        return report
    }

    private fun scheduleRetry(wait: Duration) {
        retryJob?.cancel()
        retryJob =
            scope.launch {
                delay(wait)
                requestSync()
            }
    }

    /** A failing run (e.g. a storage error) must not end the request loop. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun runQuietly() {
        try {
            syncNow()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Left pending; the next trigger tries again.
        }
    }
}
