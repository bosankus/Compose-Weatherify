package bose.ankush.home.data.events

import bose.ankush.home.data.events.sync.PlaceEventSyncOutcome
import bose.ankush.home.data.events.sync.PlaceEventSyncScheduler
import bose.ankush.home.domain.events.EventArea
import bose.ankush.home.domain.events.PlaceEventInput
import bose.ankush.home.domain.events.PlaceEventItem
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.repository.PlaceEventsRepository
import bose.ankush.network.model.CreatePlaceEventRequest
import bose.ankush.network.model.PlaceEvent
import bose.ankush.network.repository.PlaceEventRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Serves the existing nearby use cases ([bose.ankush.home.domain.usecase.GetNearbyEvents],
 * [bose.ankush.home.domain.usecase.CreateNearbyEvent]) from the offline-first store, keeping
 * their [PlaceEventRepository] contract so the home UI does not change in this step.
 *
 * - Get: the stored events within about 1 km. Before the first completed sync it waits briefly
 *   for one, so a fresh install still shows server events; otherwise it reads at once and asks
 *   for a background sync. It fails only when nothing was ever synced and that sync failed.
 * - Create: stored locally as pending and pushed by the sync, so it succeeds offline.
 */
internal class NearbyPlaceEventSource(
    private val repository: PlaceEventsRepository,
    private val sync: PlaceEventSyncScheduler,
) : PlaceEventRepository {
    /** An account with no events never gets a checkpoint, so a completed sync is remembered here too. */
    private var syncedThisSession = false

    override suspend fun getPlaceEvents(
        lat: Double,
        lon: Double,
    ): Result<List<PlaceEvent>> {
        val firstSyncFailed = !syncedThisSession && !repository.hasSynced() && !firstSyncCompleted()
        if (!firstSyncFailed) sync.requestSync()
        val events = repository.observeEvents(EventArea(GeoPoint(lat, lon))).first()
        return if (firstSyncFailed && events.isEmpty()) {
            Result.failure(IllegalStateException("Couldn't load events. Check your connection."))
        } else {
            Result.success(events.map { it.toPlaceEvent() })
        }
    }

    override suspend fun createPlaceEvent(request: CreatePlaceEventRequest): Result<Unit> {
        val startsAt =
            parseInstant(request.startsAt)
                ?: return Result.failure(IllegalArgumentException("An event needs a valid start time."))
        val input =
            PlaceEventInput(
                placeName = request.placeName,
                point = GeoPoint(request.lat, request.lon),
                title = request.title,
                startsAt = startsAt,
                note = request.note,
            )
        return repository.createEvent(input).map { }
    }

    private suspend fun firstSyncCompleted(): Boolean {
        val completed =
            withTimeoutOrNull(FIRST_SYNC_TIMEOUT) { sync.syncNow() }?.outcome == PlaceEventSyncOutcome.COMPLETED
        syncedThisSession = completed
        return completed
    }

    private companion object {
        val FIRST_SYNC_TIMEOUT = 15.seconds
    }
}

private fun PlaceEventItem.toPlaceEvent(): PlaceEvent =
    PlaceEvent(
        id = id,
        placeName = placeName,
        lat = point.lat,
        lon = point.lon,
        title = title,
        startsAt = startsAt.toString(),
        note = note,
    )
