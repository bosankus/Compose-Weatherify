package bose.ankush.home.data.events.sync

import androidx.work.WorkManager
import kotlin.time.Duration

/** The two unique place-event sync jobs, by name. An interface so the scheduler is host-testable. */
internal interface PlaceEventSyncWorkQueue {
    /** The 6-hourly job; an existing one is kept. */
    fun enqueuePeriodic()

    /** The one-time job, replacing a queued or running one: now (expedited) or after [delay]. */
    fun enqueueNow(delay: Duration? = null)

    /** Cancels both jobs. */
    fun cancelAll()
}

internal class WorkManagerPlaceEventSyncWorkQueue(
    private val workManager: () -> WorkManager,
) : PlaceEventSyncWorkQueue {
    override fun enqueuePeriodic() {
        workManager().enqueueUniquePeriodicWork(
            PlaceEventSyncWork.PERIODIC_WORK_NAME,
            PlaceEventSyncWork.PERIODIC_POLICY,
            PlaceEventSyncWork.periodicRequest(),
        )
    }

    override fun enqueueNow(delay: Duration?) {
        workManager().enqueueUniqueWork(
            PlaceEventSyncWork.ONE_TIME_WORK_NAME,
            PlaceEventSyncWork.ONE_TIME_POLICY,
            PlaceEventSyncWork.oneTimeRequest(delay),
        )
    }

    override fun cancelAll() {
        val work = workManager()
        work.cancelUniqueWork(PlaceEventSyncWork.PERIODIC_WORK_NAME)
        work.cancelUniqueWork(PlaceEventSyncWork.ONE_TIME_WORK_NAME)
    }
}
