package bose.ankush.home.data.events.sync

import bose.ankush.home.data.events.FakePlaceEventClock
import bose.ankush.home.data.events.FakePlaceEventStorage
import bose.ankush.home.data.events.FakePlaceEventSyncApi
import bose.ankush.home.data.events.FakeSyncScheduler
import bose.ankush.home.data.events.LocalFirstPlaceEventsRepository
import bose.ankush.home.data.events.PlaceEventWriteLock
import bose.ankush.home.data.events.T_SERVER
import bose.ankush.home.data.events.localRecord
import bose.ankush.home.data.events.serverDto
import bose.ankush.home.data.events.toDto
import bose.ankush.home.domain.events.PlaceEventInput
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.network.model.PlaceEventApiResult
import bose.ankush.network.model.PlaceEventSyncPage
import bose.ankush.storage.model.PlaceEventSyncState.PENDING_DELETE
import bose.ankush.storage.model.PlaceEventSyncState.PENDING_UPSERT
import bose.ankush.storage.model.PlaceEventSyncState.SYNCED
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class PlaceEventSyncEngineTest {
    private val storage = FakePlaceEventStorage()
    private val api = FakePlaceEventSyncApi()
    private val lock = PlaceEventWriteLock()
    private val clock = FakePlaceEventClock()
    private val engine = PlaceEventSyncEngine(storage, api, lock, PlaceEventSyncConfig(), clock)
    private val repository = LocalFirstPlaceEventsRepository(storage, lock, FakeSyncScheduler(), clock)

    private val input =
        PlaceEventInput(
            placeName = "Harbor",
            point = GeoPoint(12.97, 77.59),
            title = "Dinner",
            startsAt = Instant.parse("2026-10-24T14:00:00Z"),
        )

    @Test
    fun anEventCreatedOfflineIsPushedOnceOnline() =
        runTest {
            api.online = false
            val id = repository.createEvent(input).getOrThrow().id

            val offline = engine.sync()

            assertEquals(PlaceEventSyncOutcome.OFFLINE, offline.outcome)
            assertEquals(PENDING_UPSERT, storage.row(id)?.syncState)

            api.online = true
            val online = engine.sync()

            assertEquals(PlaceEventSyncOutcome.COMPLETED, online.outcome)
            assertEquals(listOf("POST $id", "GET since=null"), api.calls)
            assertEquals(id, api.created.single().id)
            assertEquals(SYNCED, storage.row(id)?.syncState)
            assertEquals(T_SERVER, storage.row(id)?.serverUpdatedAt)
        }

    @Test
    fun aPost200TombstoneRemovesTheLocalRow() =
        runTest {
            val id = repository.createEvent(input).getOrThrow().id
            api.onCreate = { body -> PlaceEventApiResult(200, body.toDto(T_SERVER).copy(deletedAt = T_SERVER)) }

            engine.sync()

            assertNull(storage.row(id))
        }

    @Test
    fun aPost200WithOlderServerContentKeepsTheLocalEditForAPut() =
        runTest {
            val id = repository.createEvent(input).getOrThrow().id
            api.onCreate = { body -> PlaceEventApiResult(200, body.toDto(T_SERVER).copy(title = "Before edit")) }

            engine.sync()

            assertEquals(PENDING_UPSERT, storage.row(id)?.syncState)
            assertEquals("Dinner", storage.row(id)?.title)
            assertEquals(T_SERVER, storage.row(id)?.serverUpdatedAt)
        }

    @Test
    fun aPost404RekeysTheRowAndRetriesOnce() =
        runTest {
            val oldId = repository.createEvent(input).getOrThrow().id
            var posts = 0
            api.onCreate = { body ->
                posts++
                if (posts == 1) PlaceEventApiResult(404) else PlaceEventApiResult(201, body.toDto(T_SERVER))
            }

            engine.sync()

            val newId = api.created.last().id
            assertEquals(2, api.created.size)
            assertNotEquals(oldId, newId)
            assertNull(storage.row(oldId))
            assertEquals(SYNCED, storage.row(newId)?.syncState)
        }

    @Test
    fun aSecondPost404LeavesTheRekeyedRowPending() =
        runTest {
            repository.createEvent(input).getOrThrow()
            api.onCreate = { PlaceEventApiResult(404) }

            engine.sync()

            assertEquals(2, api.created.size)
            assertEquals(PENDING_UPSERT, storage.row(api.created.last().id)?.syncState)
        }

    @Test
    fun aPost400LeavesTheRowPendingAndTheRunGoesOn() =
        runTest {
            val id = repository.createEvent(input).getOrThrow().id
            api.onCreate = { PlaceEventApiResult(400) }

            val report = engine.sync()

            assertEquals(PENDING_UPSERT, storage.row(id)?.syncState)
            assertEquals(PlaceEventSyncOutcome.COMPLETED, report.outcome)
            assertTrue("GET since=null" in api.calls)
        }

    @Test
    fun anEditOfASyncedRowIsPutWithItsServerBase() =
        runTest {
            storage.saveEvent(localRecord("a", SYNCED, serverUpdatedAt = T_SERVER))
            repository.updateEvent("a", input.copy(title = "Lunch")).getOrThrow()

            engine.sync()

            assertEquals(
                T_SERVER,
                api.updated
                    .single()
                    .second.baseUpdatedAt,
            )
            assertEquals(
                "Lunch",
                api.updated
                    .single()
                    .second.title,
            )
            assertEquals(SYNCED, storage.row("a")?.syncState)
        }

    @Test
    fun aPut409TakesTheServerCopy() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_UPSERT, serverUpdatedAt = T_SERVER, title = "Mine"))
            val current = serverDto("a", updatedAt = "2026-10-22T09:30:00.000Z", title = "Theirs")
            api.onUpdate = { _, _ -> PlaceEventApiResult(409, current) }

            engine.sync()

            assertEquals("Theirs", storage.row("a")?.title)
            assertEquals(SYNCED, storage.row("a")?.syncState)
            assertEquals("2026-10-22T09:30:00.000Z", storage.row("a")?.serverUpdatedAt)
        }

    @Test
    fun aPut404DropsTheLocalRow() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_UPSERT, serverUpdatedAt = T_SERVER))
            api.onUpdate = { _, _ -> PlaceEventApiResult(404) }

            engine.sync()

            assertNull(storage.row("a"))
        }

    @Test
    fun anEditMadeWhileItsPutIsInFlightStaysPendingOnTheNewBase() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_UPSERT, serverUpdatedAt = T_SERVER))
            api.onUpdate = { id, body ->
                repository.updateEvent("a", input.copy(title = "Newer"))
                PlaceEventApiResult(200, body.toDto(id, "2026-10-22T09:45:00.000Z"))
            }

            engine.sync()

            assertEquals("Newer", storage.row("a")?.title)
            assertEquals(PENDING_UPSERT, storage.row("a")?.syncState)
            assertEquals("2026-10-22T09:45:00.000Z", storage.row("a")?.serverUpdatedAt)
        }

    @Test
    fun deleteTreats204And404AsDone() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_DELETE, updatedAt = "2026-10-22T10:00:01Z"))
            storage.saveEvent(localRecord("b", PENDING_DELETE, updatedAt = "2026-10-22T10:00:02Z"))
            api.onDelete = { id -> PlaceEventApiResult(if (id == "a") 204 else 404) }

            val report = engine.sync()

            assertEquals(listOf("DELETE a", "DELETE b"), api.calls.take(2))
            assertTrue(storage.rows.value.isEmpty())
            assertEquals(2, report.pushed)
        }

    @Test
    fun aPulledTombstoneRemovesTheLocalRow() =
        runTest {
            storage.saveEvent(localRecord("a", SYNCED, serverUpdatedAt = T_SERVER))
            api.onChanges = { since ->
                page(
                    since == null,
                    listOf(serverDto("a", "2026-10-22T09:10:00.000Z", deletedAt = "2026-10-22T09:10:00.000Z")),
                )
            }

            engine.sync()

            assertNull(storage.row("a"))
        }

    @Test
    fun pullingFollowsNextSinceStoresTheCheckpointThenOverlapsTenSeconds() =
        runTest {
            storage.checkpoint = "2026-10-22T08:00:00.000Z~0123456789abcdef01234567"
            val last = "2026-10-22T10:00:20.500Z~0123456789abcdef0123abcd"
            api.onChanges = { since ->
                when (since) {
                    "2026-10-22T08:00:00.000Z~0123456789abcdef01234567" ->
                        PlaceEventApiResult(
                            200,
                            PlaceEventSyncPage(
                                listOf(serverDto("a")),
                                nextSince = "c1",
                                checkpoint = "c1",
                                hasMore = true,
                            ),
                        )
                    "c1" ->
                        PlaceEventApiResult(
                            200,
                            PlaceEventSyncPage(
                                listOf(serverDto("b")),
                                nextSince = null,
                                checkpoint = last,
                                hasMore = false,
                            ),
                        )
                    else -> PlaceEventApiResult(200, PlaceEventSyncPage(listOf(serverDto("b")), checkpoint = "ignored"))
                }
            }

            val report = engine.sync()

            assertEquals(
                listOf(
                    "GET since=2026-10-22T08:00:00.000Z~0123456789abcdef01234567",
                    "GET since=c1",
                    "GET since=2026-10-22T10:00:10.500Z",
                ),
                api.calls,
            )
            assertEquals(last, storage.checkpoint)
            assertEquals(setOf("a", "b"), storage.rows.value.keys)
            assertEquals(PlaceEventSyncOutcome.COMPLETED, report.outcome)
        }

    @Test
    fun aPullNeverClobbersANewerPendingEditButReplacesAnOlderOne() =
        runTest {
            storage.saveEvent(localRecord("newer", PENDING_UPSERT, updatedAt = "2026-10-22T09:30:00Z", title = "Mine"))
            storage.saveEvent(localRecord("older", PENDING_UPSERT, updatedAt = "2026-10-22T08:30:00Z", title = "Mine"))
            storage.saveEvent(localRecord("gone", PENDING_DELETE, updatedAt = "2026-10-22T08:30:00Z"))
            api.onCreate = { PlaceEventApiResult(400) }
            api.onDelete = { PlaceEventApiResult(400) }
            api.onChanges = { since ->
                page(since == null, listOf(serverDto("newer"), serverDto("older"), serverDto("gone", title = "Back")))
            }

            engine.sync() // creates answer 400 (kept pending); the pull still runs

            assertEquals("Mine", storage.row("newer")?.title)
            assertEquals(PENDING_UPSERT, storage.row("newer")?.syncState)
            assertEquals("Server", storage.row("older")?.title)
            assertEquals(SYNCED, storage.row("older")?.syncState)
            assertEquals(PENDING_DELETE, storage.row("gone")?.syncState)
        }

    @Test
    fun aRunStopsAtFortyRequestsAndLeavesTheRestPending() =
        runTest {
            repeat(50) { i ->
                storage.saveEvent(
                    localRecord(
                        "e$i",
                        PENDING_DELETE,
                        updatedAt = "2026-10-22T10:00:${(i % 60).toString().padStart(2, '0')}Z",
                    ),
                )
            }

            val report = engine.sync()

            assertEquals(40, api.calls.size)
            assertEquals(40, report.requests)
            assertEquals(PlaceEventSyncOutcome.REQUEST_CAP, report.outcome)
            assertEquals(10, storage.rows.value.size)
            assertEquals(1.minutes, report.retryAfter)
        }

    @Test
    fun aRateLimitHonoursRetryAfterAndLeavesTheRestPending() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_DELETE, updatedAt = "2026-10-22T10:00:01Z"))
            storage.saveEvent(localRecord("b", PENDING_DELETE, updatedAt = "2026-10-22T10:00:02Z"))
            storage.saveEvent(localRecord("c", PENDING_DELETE, updatedAt = "2026-10-22T10:00:03Z"))
            api.onDelete =
                { id -> if (id == "b") PlaceEventApiResult(429, retryAfterSeconds = 17) else PlaceEventApiResult(204) }

            val report = engine.sync()

            assertEquals(listOf("DELETE a", "DELETE b"), api.calls)
            assertEquals(PlaceEventSyncOutcome.RATE_LIMITED, report.outcome)
            assertEquals(17.seconds, report.retryAfter)
            assertEquals(setOf("b", "c"), storage.rows.value.keys)
        }

    @Test
    fun aRateLimitWithoutRetryAfterBacksOffExponentially() =
        runTest {
            api.onChanges = { PlaceEventApiResult(429) }

            val first = engine.sync()
            val second = engine.sync()
            val third = engine.sync()

            assertEquals(listOf(2.seconds, 4.seconds, 8.seconds), listOf(first, second, third).map { it.retryAfter })
        }

    @Test
    fun aClearDuringARunDropsItsResults() =
        runTest {
            api.onChanges = {
                repository.clearAll()
                page(true, listOf(serverDto("a")))
            }

            val report = engine.sync()

            assertEquals(PlaceEventSyncOutcome.CLEARED, report.outcome)
            assertTrue(storage.rows.value.isEmpty())
            assertNull(storage.checkpoint)
        }

    private fun page(
        first: Boolean,
        items: List<bose.ankush.network.model.PlaceEventDto>,
    ): PlaceEventApiResult<PlaceEventSyncPage> =
        PlaceEventApiResult(
            200,
            if (first) {
                PlaceEventSyncPage(items, checkpoint = "2026-10-22T09:59:00.000Z~0123456789abcdef01234567")
            } else {
                PlaceEventSyncPage(emptyList(), checkpoint = "2026-10-22T09:59:00.000Z~0123456789abcdef01234567")
            },
        )
}
