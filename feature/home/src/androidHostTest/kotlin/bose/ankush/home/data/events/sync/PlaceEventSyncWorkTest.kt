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
import kotlin.time.Duration
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
        val failed = report(PlaceEventSyncOutcome.SERVER_ERROR, retryAfter = 2.seconds)
        val done = report(PlaceEventSyncOutcome.COMPLETED, retryAfter = null)

        assertEquals(WorkOutcome.Retry, PlaceEventSyncWork.outcomeFor(failed, runAttemptCount = 0))
        assertEquals(WorkOutcome.Retry, PlaceEventSyncWork.outcomeFor(failed, runAttemptCount = 4))
        assertEquals(WorkOutcome.Success, PlaceEventSyncWork.outcomeFor(failed, runAttemptCount = 5))
        assertEquals(WorkOutcome.Success, PlaceEventSyncWork.outcomeFor(done, runAttemptCount = 0))
        assertEquals(WorkOutcome.Success, PlaceEventSyncWork.outcomeFor(null, runAttemptCount = 0))
    }

    @Test
    fun aRetryAfterLongerThanTheNextBackoffReschedulesTheOneTimeJob() {
        val limited = rateLimited(serverRetryAfter = 120.seconds)

        assertEquals(WorkOutcome.Reschedule(120.seconds), PlaceEventSyncWork.outcomeFor(limited, runAttemptCount = 0))
        assertEquals(WorkOutcome.Reschedule(120.seconds), PlaceEventSyncWork.outcomeFor(limited, runAttemptCount = 1))
    }

    @Test
    fun aRetryAfterWithinTheNextBackoffUsesWorkManagersRetry() {
        assertEquals(WorkOutcome.Retry, PlaceEventSyncWork.outcomeFor(rateLimited(20.seconds), runAttemptCount = 0))
        assertEquals(WorkOutcome.Retry, PlaceEventSyncWork.outcomeFor(rateLimited(30.seconds), runAttemptCount = 0))
        // Run 2 retries after 120 s, which already covers a 100 s Retry-After.
        assertEquals(WorkOutcome.Retry, PlaceEventSyncWork.outcomeFor(rateLimited(100.seconds), runAttemptCount = 2))
        // A 429 without Retry-After, and a server error, stay on the backoff.
        assertEquals(WorkOutcome.Retry, PlaceEventSyncWork.outcomeFor(rateLimited(null), runAttemptCount = 0))
        assertEquals(WorkOutcome.Success, PlaceEventSyncWork.outcomeFor(rateLimited(600.seconds), runAttemptCount = 5))
    }

    @Test
    fun theBackoffMatchesWorkManagersExponentialPolicy() {
        assertEquals(30.seconds, PlaceEventSyncWork.backoffAfter(0))
        assertEquals(60.seconds, PlaceEventSyncWork.backoffAfter(1))
        assertEquals(240.seconds, PlaceEventSyncWork.backoffAfter(3))
        assertEquals(5.hours, PlaceEventSyncWork.backoffAfter(30))
    }

    private fun rateLimited(serverRetryAfter: Duration?) =
        PlaceEventSyncReport(
            PlaceEventSyncOutcome.RATE_LIMITED,
            requests = 1,
            pushed = 0,
            pulled = 0,
            retryAfter = serverRetryAfter ?: 2.seconds,
            serverRetryAfter = serverRetryAfter,
        )

    private fun report(
        outcome: PlaceEventSyncOutcome,
        retryAfter: Duration?,
    ) = PlaceEventSyncReport(outcome, requests = 1, pushed = 0, pulled = 0, retryAfter = retryAfter)
}
