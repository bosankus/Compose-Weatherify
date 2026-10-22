package bose.ankush.home.data.events.sync

import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/** The WorkManager requests behind Android place-event sync, built without a device. */
class PlaceEventSyncWorkTest {
    @Test
    fun aSyncNowIsExpeditedWithTheQuotaFallbackAndNeedsAConnection() {
        val spec = PlaceEventSyncWork.oneTimeRequest().workSpec

        assertTrue(spec.expedited)
        assertEquals(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST, spec.outOfQuotaPolicy)
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertEquals(0L, spec.initialDelay)
        assertEquals(PlaceEventSyncWorker::class.java.name, spec.workerClassName)
        assertEquals(BackoffPolicy.EXPONENTIAL, spec.backoffPolicy)
        assertEquals(30.seconds.inWholeMilliseconds, spec.backoffDelayDuration)
    }

    @Test
    fun aDelayedRetryIsPlainWorkWithTheWait() {
        val spec = PlaceEventSyncWork.oneTimeRequest(delay = 17.seconds).workSpec

        assertFalse(spec.expedited)
        assertEquals(17_000L, spec.initialDelay)
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
    }

    @Test
    fun thePeriodicSyncRunsEverySixHoursWhileConnected() {
        val spec = PlaceEventSyncWork.periodicRequest().workSpec

        assertTrue(spec.isPeriodic)
        assertEquals(6.hours.inWholeMilliseconds, spec.intervalDuration)
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertEquals(PlaceEventSyncWorker::class.java.name, spec.workerClassName)
    }

    @Test
    fun uniqueWorkKeepsThePeriodicJobAndCoalescesWritesIntoOneRun() {
        assertEquals(ExistingPeriodicWorkPolicy.KEEP, PlaceEventSyncWork.PERIODIC_POLICY)
        assertEquals(ExistingWorkPolicy.REPLACE, PlaceEventSyncWork.ONE_TIME_POLICY)
        assertNotEquals(PlaceEventSyncWork.PERIODIC_WORK_NAME, PlaceEventSyncWork.ONE_TIME_WORK_NAME)
    }

    @Test
    fun aRunRetriesOnlyWhenTheEngineAsksAndAttemptsRemain() {
        val failed = report(PlaceEventSyncOutcome.SERVER_ERROR, retryAfter = true)
        val done = report(PlaceEventSyncOutcome.COMPLETED, retryAfter = false)

        assertEquals(WorkOutcome.RETRY, PlaceEventSyncWork.outcomeFor(failed, runAttemptCount = 0))
        assertEquals(WorkOutcome.RETRY, PlaceEventSyncWork.outcomeFor(failed, runAttemptCount = 4))
        assertEquals(WorkOutcome.SUCCESS, PlaceEventSyncWork.outcomeFor(failed, runAttemptCount = 5))
        assertEquals(WorkOutcome.SUCCESS, PlaceEventSyncWork.outcomeFor(done, runAttemptCount = 0))
        assertEquals(WorkOutcome.SUCCESS, PlaceEventSyncWork.outcomeFor(null, runAttemptCount = 0))
    }

    private fun report(
        outcome: PlaceEventSyncOutcome,
        retryAfter: Boolean,
    ) = PlaceEventSyncReport(
        outcome,
        requests = 1,
        pushed = 0,
        pulled = 0,
        retryAfter = if (retryAfter) 2.seconds else null,
    )
}
