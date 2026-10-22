package bose.ankush.home.data.events.sync

import androidx.work.WorkManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

/**
 * Android place-event sync on WorkManager, so pending writes reach the server even after the
 * process is gone. [requestSync] (every write and app start) makes sure the periodic job exists
 * and enqueues the expedited one-time job; both need a connection.
 *
 * The one-time job uses [PlaceEventSyncWork.ONE_TIME_POLICY] = REPLACE rather than
 * APPEND_OR_REPLACE: a run reads every pending row when it starts, so one queued run covers any
 * number of writes. APPEND would chain one extra run per write made during a run, each costing
 * a pull against the server's 60-requests-a-minute limit. Replacing a run that is in flight is
 * safe: pushes are idempotent (client ids on POST, `baseUpdatedAt` on PUT), each row is written
 * on its own, and the engine runs one sync at a time.
 */
internal class WorkManagerPlaceEventSyncScheduler(
    private val workManager: () -> WorkManager,
    private val engine: PlaceEventSyncEngine,
    private val isSignedIn: suspend () -> Boolean,
) : PlaceEventSyncScheduler {
    private val periodicEnqueued = AtomicBoolean(false)

    override fun requestSync() {
        val work = workManager()
        if (periodicEnqueued.compareAndSet(false, true)) {
            work.enqueueUniquePeriodicWork(
                PlaceEventSyncWork.PERIODIC_WORK_NAME,
                PlaceEventSyncWork.PERIODIC_POLICY,
                PlaceEventSyncWork.periodicRequest(),
            )
        }
        enqueueOneTime(delay = null)
    }

    /**
     * Syncs in the caller's coroutine (the first events load). A run that asks to be retried is
     * handed to WorkManager with its wait (Retry-After or backoff).
     */
    override suspend fun syncNow(): PlaceEventSyncReport? {
        val report = syncInWorker()
        report?.retryAfter?.let(::enqueueOneTime)
        return report
    }

    /**
     * One run of the shared engine for [PlaceEventSyncWorker]; null while signed out. A retry is
     * left to WorkManager's backoff, which the worker asks for.
     */
    suspend fun syncInWorker(): PlaceEventSyncReport? = if (isSignedIn()) engine.sync() else null

    private fun enqueueOneTime(delay: Duration?) {
        workManager().enqueueUniqueWork(
            PlaceEventSyncWork.ONE_TIME_WORK_NAME,
            PlaceEventSyncWork.ONE_TIME_POLICY,
            PlaceEventSyncWork.oneTimeRequest(delay),
        )
    }
}
