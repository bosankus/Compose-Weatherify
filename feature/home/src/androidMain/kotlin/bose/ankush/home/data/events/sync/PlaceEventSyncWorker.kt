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
 */
internal class PlaceEventSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val scheduler =
            GlobalContext.getOrNull()?.getOrNull<WorkManagerPlaceEventSyncScheduler>() ?: return Result.retry()
        val outcome = PlaceEventSyncWork.outcomeFor(syncQuietly(scheduler), runAttemptCount)
        return if (outcome == WorkOutcome.RETRY) Result.retry() else Result.success()
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
