package bose.ankush.home.domain.usecase

import bose.ankush.home.domain.events.EventArea
import bose.ankush.home.domain.events.PlaceEventInput
import bose.ankush.home.domain.events.PlaceEventItem
import bose.ankush.home.domain.repository.PlaceEventsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/** Live place events around an [EventArea], from the offline-first store. */
internal class ObservePlaceEvents(
    private val repository: PlaceEventsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    operator fun invoke(area: EventArea): Flow<List<PlaceEventItem>> =
        repository.observeEvents(area).flowOn(ioDispatcher)
}

/**
 * Edits a place event locally (pending, which re-queues one that couldn't sync) and schedules a
 * sync. A blank title is refused.
 */
internal class UpdatePlaceEvent(
    private val repository: PlaceEventsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(
        id: String,
        input: PlaceEventInput,
    ): Result<PlaceEventItem> =
        if (input.title.isBlank()) {
            Result.failure(IllegalArgumentException("An event needs a title."))
        } else {
            withContext(ioDispatcher) { repository.updateEvent(id, input) }
        }
}

/** Deletes a place event locally (hidden at once) and schedules a sync. */
internal class DeletePlaceEvent(
    private val repository: PlaceEventsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(id: String): Result<Unit> = withContext(ioDispatcher) { repository.deleteEvent(id) }
}

/** Drops a change the server refused, so the event goes back to the server's copy (or away). */
internal class DiscardFailedPlaceEvent(
    private val repository: PlaceEventsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(id: String): Result<Unit> =
        withContext(ioDispatcher) { repository.discardFailedEvent(id) }
}
