package bose.ankush.home.data.events

import bose.ankush.home.data.events.sync.PlaceEventSyncOutcome
import bose.ankush.home.data.events.sync.PlaceEventSyncReport
import bose.ankush.network.model.CreatePlaceEventRequest
import bose.ankush.storage.model.PlaceEventSyncState.PENDING_UPSERT
import bose.ankush.storage.model.PlaceEventSyncState.SYNCED
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The existing nearby use cases' contract, served from the offline-first store. */
class NearbyPlaceEventSourceTest {
    private val storage = FakePlaceEventStorage()
    private val scheduler = FakeSyncScheduler()
    private val repository =
        LocalFirstPlaceEventsRepository(storage, PlaceEventWriteLock(), scheduler, FakePlaceEventClock())
    private val source = NearbyPlaceEventSource(repository, scheduler)

    @Test
    fun theFirstLoadWaitsForASyncThenReadsTheStore() =
        runTest {
            storage.saveEvent(localRecord("a", SYNCED))
            scheduler.report = PlaceEventSyncReport(PlaceEventSyncOutcome.COMPLETED, 1, 0, 1)

            val events = source.getPlaceEvents(12.97, 77.59).getOrThrow()

            assertEquals(1, scheduler.syncNowCalls)
            assertEquals(listOf("a"), events.map { it.id })
        }

    @Test
    fun laterLoadsReadAtOnceAndSyncInTheBackground() =
        runTest {
            storage.checkpoint = "c"

            source.getPlaceEvents(12.97, 77.59).getOrThrow()

            assertEquals(0, scheduler.syncNowCalls)
            assertEquals(1, scheduler.requests)
        }

    @Test
    fun aLoadFailsOnlyWhenNothingWasEverSyncedAndTheSyncFailed() =
        runTest {
            scheduler.report = PlaceEventSyncReport(PlaceEventSyncOutcome.OFFLINE, 1, 0, 0)

            assertTrue(source.getPlaceEvents(12.97, 77.59).isFailure)

            repository.createEvent(
                bose.ankush.home.domain.events.PlaceEventInput(
                    "Harbor",
                    bose.ankush.home.domain.nearby
                        .GeoPoint(12.97, 77.59),
                    "Offline",
                    kotlin.time.Instant.parse("2026-10-24T14:00:00Z"),
                ),
            )
            assertEquals(listOf("Offline"), source.getPlaceEvents(12.97, 77.59).getOrThrow().map { it.title })
        }

    @Test
    fun aCreateIsStoredPendingWithItsNote() =
        runTest {
            source
                .createPlaceEvent(
                    CreatePlaceEventRequest(
                        "Harbor",
                        12.97,
                        77.59,
                        "Dinner",
                        "2026-10-24T14:00:00Z",
                        note = "Window seat",
                    ),
                ).getOrThrow()

            val row =
                storage.rows.value.values
                    .single()
            assertEquals(PENDING_UPSERT, row.syncState)
            assertEquals("Window seat", row.note)
            assertEquals(1, scheduler.requests)
        }

    @Test
    fun aCreateWithAnUnreadableStartIsRefused() =
        runTest {
            assertTrue(source.createPlaceEvent(CreatePlaceEventRequest("Harbor", 1.0, 2.0, "Dinner", "soon")).isFailure)
            assertTrue(storage.rows.value.isEmpty())
        }
}
