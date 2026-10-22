package bose.ankush.home.data.events.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkRequest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * The WorkManager jobs behind place-event sync on Android, as pure request builders so they can
 * be checked without a device:
 * - [PERIODIC_WORK_NAME]: every [PERIODIC_INTERVAL] while connected, kept across app starts
 *   ([PERIODIC_POLICY] = KEEP).
 * - [ONE_TIME_WORK_NAME]: one expedited run per write or app start, while connected.
 *   [ONE_TIME_POLICY] is REPLACE so any number of writes coalesce into a single queued run (see
 *   [WorkManagerPlaceEventSyncScheduler]).
 */
internal object PlaceEventSyncWork {
    const val PERIODIC_WORK_NAME = "place-events-sync-periodic"
    const val ONE_TIME_WORK_NAME = "place-events-sync-now"
    val PERIODIC_INTERVAL: Duration = 6.hours
    val PERIODIC_POLICY = ExistingPeriodicWorkPolicy.KEEP
    val ONE_TIME_POLICY = ExistingWorkPolicy.REPLACE

    /** First retry wait after a failed run (429, offline, 5xx, the request cap); doubles each time. */
    val RETRY_BACKOFF: Duration = 30.seconds

    /** Runs of one request that may end in retry; after that it waits for the next trigger. */
    const val MAX_ATTEMPTS = 6

    private val connected: Constraints =
        Constraints
            .Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    /**
     * A sync now, expedited, falling back to ordinary work when the app is out of expedited quota.
     * With [delay] (a server Retry-After or backoff), a plain delayed run instead: expedited work
     * cannot be delayed.
     */
    fun oneTimeRequest(delay: Duration? = null): OneTimeWorkRequest {
        val builder =
            OneTimeWorkRequestBuilder<PlaceEventSyncWorker>()
                .setConstraints(connected)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF.toJavaDuration())
        if (delay != null && delay.isPositive()) {
            builder.setInitialDelay(delay.toJavaDuration())
        } else {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }
        return builder.build()
    }

    fun periodicRequest(): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<PlaceEventSyncWorker>(PERIODIC_INTERVAL.toJavaDuration())
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF.toJavaDuration())
            .build()

    /**
     * What a worker run reports to WorkManager:
     * - done when the engine asked for no retry, or the attempts are used up (signed out is done);
     * - after a 429 whose `Retry-After` is longer than WorkManager's next backoff, done now and
     *   the one-time job enqueued again with that wait ([WorkOutcome.Reschedule]);
     * - otherwise retry on WorkManager's backoff.
     */
    fun outcomeFor(
        report: PlaceEventSyncReport?,
        runAttemptCount: Int,
    ): WorkOutcome {
        val serverWait = report?.serverRetryAfter?.takeIf { report.outcome == PlaceEventSyncOutcome.RATE_LIMITED }
        return when {
            report?.retryAfter == null || runAttemptCount >= MAX_ATTEMPTS - 1 -> WorkOutcome.Success
            serverWait != null && serverWait > backoffAfter(runAttemptCount) -> WorkOutcome.Reschedule(serverWait)
            else -> WorkOutcome.Retry
        }
    }

    /**
     * The wait WorkManager applies when run number [runAttemptCount] (0-based) returns retry:
     * [RETRY_BACKOFF] doubled per earlier attempt, capped at WorkManager's maximum.
     */
    fun backoffAfter(runAttemptCount: Int): Duration {
        val doublings = runAttemptCount.coerceIn(0, MAX_DOUBLINGS)
        return (RETRY_BACKOFF * (1L shl doublings).toDouble()).coerceAtMost(WorkRequest.MAX_BACKOFF_MILLIS.milliseconds)
    }

    private const val MAX_DOUBLINGS = 20
}

/** A worker run's answer to WorkManager. */
internal sealed interface WorkOutcome {
    data object Success : WorkOutcome

    data object Retry : WorkOutcome

    /** Done now; the one-time job is enqueued again to start after [delay]. */
    data class Reschedule(
        val delay: Duration,
    ) : WorkOutcome
}
