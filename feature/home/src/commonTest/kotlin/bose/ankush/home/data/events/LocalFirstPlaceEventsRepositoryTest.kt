package bose.ankush.home.data.events

import bose.ankush.home.domain.events.EventArea
import bose.ankush.home.domain.events.PlaceEventInput
import bose.ankush.home.domain.events.PlaceEventSyncStatus
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.storage.model.PlaceEventSyncState.FAILED
import bose.ankush.storage.model.PlaceEventSyncState.PENDING_DELETE
import bose.ankush.storage.model.PlaceEventSyncState.PENDING_UPSERT
import bose.ankush.storage.model.PlaceEventSyncState.SYNCED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LocalFirstPlaceEventsRepositoryTest {
    private val storage = FakePlaceEventStorage()
    private val scheduler = FakeSyncScheduler()
    private val repository =
        LocalFirstPlaceEventsRepository(storage, PlaceEventWriteLock(), scheduler, FakePlaceEventClock())

    private val harbor = GeoPoint(12.9716, 77.5946)
    private val input =
        PlaceEventInput(
            placeName = "Harbor",
            point = harbor,
            title = " Dinner ",
            startsAt = Instant.parse("2026-10-24T14:00:00Z"),
            note = "  ",
        )

    @Test
    fun aCreateIsWrittenLocallyFirstAsPendingThenAsksForASync() =
        runTest {
            val item = repository.createEvent(input).getOrThrow()

            val row = storage.row(item.id)
            assertEquals(PENDING_UPSERT, row?.syncState)
            assertNull(row?.serverUpdatedAt)
            assertEquals("Dinner", row?.title)
            assertNull(row?.note)
            assertEquals("2026-10-24T14:00:00Z", row?.startsAt)
            assertTrue(item.isPendingSync)
            assertTrue(Regex("^[0-9a-f-]{36}$").matches(item.id))
            assertEquals(1, scheduler.requests)
        }

    @Test
    fun observeKeepsOnlyEventsWithinTheRadius() =
        runTest {
            storage.saveEvent(localRecord("near", SYNCED).copy(lat = 12.9760, lon = 77.5946)) // ~0.5 km
            storage.saveEvent(localRecord("far", SYNCED).copy(lat = 12.9900, lon = 77.5946)) // ~2 km

            val events = repository.observeEvents(EventArea(harbor)).first()

            assertEquals(listOf("near"), events.map { it.id })
            assertEquals(
                listOf("near", "far"),
                repository.observeEvents(EventArea(harbor, 3_000.0)).first().map { it.id },
            )
        }

    @Test
    fun observeEmitsLocalWritesAndHidesDeletes() =
        runTest {
            val seen = mutableListOf<List<String>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeEvents(EventArea(harbor)).collect { events -> seen += events.map { it.id } }
            }

            val id = repository.createEvent(input).getOrThrow().id
            repository.deleteEvent(id).getOrThrow()

            assertEquals(listOf(emptyList(), listOf(id), emptyList()), seen)
        }

    @Test
    fun deletingAnEventTheServerNeverAcknowledgedRemovesItLocallyAndAsksForNoSync() =
        runTest {
            val id = repository.createEvent(input).getOrThrow().id

            repository.deleteEvent(id).getOrThrow()

            assertNull(storage.row(id))
            assertEquals(1, scheduler.requests) // the create only
        }

    @Test
    fun deletingASyncedEventQueuesADeleteForTheServer() =
        runTest {
            storage.saveEvent(localRecord("a", SYNCED, serverUpdatedAt = T_SERVER))

            repository.deleteEvent("a").getOrThrow()

            assertEquals(PENDING_DELETE, storage.row("a")?.syncState)
            assertEquals(1, scheduler.requests)
            assertTrue(repository.observeEvents(EventArea(harbor)).first().isEmpty())
        }

    @Test
    fun aFailedRowIsShownWithItsServerMessage() =
        runTest {
            storage.saveEvent(localRecord("a", FAILED).copy(syncError = "title is too long"))

            val item = repository.observeEvents(EventArea(harbor)).first().single()

            assertEquals(PlaceEventSyncStatus.FAILED, item.syncStatus)
            assertEquals("title is too long", item.syncError)
            assertFalse(item.isPendingSync)
        }

    @Test
    fun anEditQueuesAFailedRowAgainAndClearsItsMessage() =
        runTest {
            storage.saveEvent(localRecord("a", FAILED, serverUpdatedAt = T_SERVER).copy(syncError = "bad"))

            val item = repository.updateEvent("a", input.copy(title = "Fixed")).getOrThrow()

            assertEquals(PENDING_UPSERT, storage.row("a")?.syncState)
            assertNull(storage.row("a")?.syncError)
            assertEquals(PlaceEventSyncStatus.PENDING, item.syncStatus)
            assertEquals(1, scheduler.requests)
        }

    @Test
    fun discardingAFailedCreateDropsItWithNoSync() =
        runTest {
            storage.saveEvent(localRecord("a", FAILED).copy(syncError = "bad"))
            storage.checkpoint = "c"

            repository.discardFailedEvent("a").getOrThrow()

            assertNull(storage.row("a"))
            assertEquals("c", storage.checkpoint)
            assertEquals(0, scheduler.requests)
        }

    @Test
    fun discardingAFailedEditResetsTheCheckpointToFetchTheServerCopyBack() =
        runTest {
            storage.saveEvent(localRecord("a", FAILED, serverUpdatedAt = T_SERVER).copy(syncError = "bad"))
            storage.checkpoint = "c"

            repository.discardFailedEvent("a").getOrThrow()

            assertNull(storage.row("a"))
            assertNull(storage.checkpoint)
            assertEquals(1, scheduler.requests)
        }

    @Test
    fun onlyAFailedRowCanBeDiscarded() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_UPSERT))

            assertTrue(repository.discardFailedEvent("a").isFailure)
            assertTrue(repository.discardFailedEvent("missing").isFailure)
            assertEquals(PENDING_UPSERT, storage.row("a")?.syncState)
        }

    @Test
    fun anEditMarksTheRowPendingAndKeepsItsServerBase() =
        runTest {
            storage.saveEvent(localRecord("a", SYNCED, serverUpdatedAt = T_SERVER))

            repository.updateEvent("a", input.copy(title = "Lunch")).getOrThrow()

            val row = storage.row("a")
            assertEquals("Lunch", row?.title)
            assertEquals(PENDING_UPSERT, row?.syncState)
            assertEquals(T_SERVER, row?.serverUpdatedAt)
            assertEquals(1, scheduler.requests)
        }

    @Test
    fun editingAnUnknownOrDeletedEventFails() =
        runTest {
            storage.saveEvent(localRecord("gone", PENDING_DELETE))

            assertTrue(repository.updateEvent("missing", input).isFailure)
            assertTrue(repository.updateEvent("gone", input).isFailure)
        }

    @Test
    fun clearAllDropsEventsAndTheCheckpoint() =
        runTest {
            storage.saveEvent(localRecord("a", SYNCED))
            storage.checkpoint = "c"

            repository.clearAll()

            assertTrue(storage.rows.value.isEmpty())
            assertNull(storage.checkpoint)
            assertTrue(!repository.hasSynced())
        }
}
