package bose.ankush.home.data.events.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Android place-event sync on WorkManager, so pending writes reach the server even after the
 * process is gone. Signing in enqueues the periodic job (KEEP) and one immediate run; signing
 * out cancels both by name. [requestSync] (every write and app start) enqueues the expedited
 * one-time job. Both need a connection.
 *
 * The one-time job uses [PlaceEventSyncWork.ONE_TIME_POLICY] = REPLACE rather than
 * APPEND_OR_REPLACE: a run reads every pending row when it starts, so one queued run covers any
 * number of writes. APPEND would chain one extra run per write made during a run, each costing
 * a pull against the server's 60-requests-a-minute limit. Replacing a run that is in flight is
 * safe: pushes are idempotent (client ids on POST, `baseUpdatedAt` on PUT), each row is written
 * on its own, and the engine runs one sync at a time.
 */
internal class WorkManagerPlaceEventSyncScheduler(
    private val queue: PlaceEventSyncWorkQueue,
    private val engine: PlaceEventSyncEngine,
    private val isSignedIn: suspend () -> Boolean,
    signedInChanges: Flow<Boolean>,
    scope: CoroutineScope,
) : PlaceEventSyncScheduler {
    init {
        // The app's session state (a stored JWT). Signing in starts both jobs; signing out
        // cancels them, so nothing runs for an account that is gone.
        scope.launch {
            signedInChanges.distinctUntilChanged().collect { signedIn ->
                if (signedIn) {
                    queue.enqueuePeriodic()
                    queue.enqueueNow()
                } else {
                    queue.cancelAll()
                }
            }
        }
    }

    override fun requestSync() = queue.enqueueNow()

    /**
     * Syncs in the caller's coroutine (the first events load). A run that asks to be retried is
     * handed to WorkManager with its wait (Retry-After or backoff).
     */
    override suspend fun syncNow(): PlaceEventSyncReport? {
        val report = syncInWorker()
        report?.retryAfter?.let(::retryLater)
        return report
    }

    /**
     * One run of the shared engine for [PlaceEventSyncWorker]; null while signed out. The worker
     * decides how to retry (see [PlaceEventSyncWork.outcomeFor]).
     */
    suspend fun syncInWorker(): PlaceEventSyncReport? = if (isSignedIn()) engine.sync() else null

    /** Enqueues the one-time job again to start after [delay] (REPLACE). */
    fun retryLater(delay: Duration) = queue.enqueueNow(delay)
}
