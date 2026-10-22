package bose.ankush.home.data.events.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import org.koin.core.context.GlobalContext

/**
 * Runs one place-event sync for WorkManager through the app's Koin graph. Created by the default
 * WorkerFactory (no custom WorkManager configuration needed): it only reads Koin when it runs,
 * and retries if Koin is not started yet (a run started before `Application.onCreate` finished).
 * A failed run retries on WorkManager's backoff, or after a longer 429 `Retry-After`
 * ([PlaceEventSyncWork.outcomeFor]).
 */
internal class PlaceEventSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val scheduler =
            GlobalContext.getOrNull()?.getOrNull<WorkManagerPlaceEventSyncScheduler>() ?: return Result.retry()
        return when (val outcome = PlaceEventSyncWork.outcomeFor(syncQuietly(scheduler), runAttemptCount)) {
            WorkOutcome.Success -> Result.success()
            WorkOutcome.Retry -> Result.retry()
            is WorkOutcome.Reschedule -> {
                // A long Retry-After: start over after it rather than on the shorter backoff.
                scheduler.retryLater(outcome.delay)
                Result.success()
            }
        }
    }

    /** A run that throws (e.g. a storage error) counts as a failed run; cancellation (REPLACE) is not caught. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun syncQuietly(scheduler: WorkManagerPlaceEventSyncScheduler): PlaceEventSyncReport? =
        try {
            scheduler.syncInWorker()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            FAILED_RUN
        }

    private companion object {
        /** Stands in for a run that threw, so it is retried like any failed run. */
        val FAILED_RUN =
            PlaceEventSyncReport(
                outcome = PlaceEventSyncOutcome.OFFLINE,
                requests = 0,
                pushed = 0,
                pulled = 0,
                retryAfter = PlaceEventSyncWork.RETRY_BACKOFF,
            )
    }
}
