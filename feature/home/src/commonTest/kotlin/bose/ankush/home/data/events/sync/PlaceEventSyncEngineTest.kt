package bose.ankush.home.data.events.sync

import bose.ankush.home.data.events.FakeOfflineException
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
import bose.ankush.home.domain.events.EventArea
import bose.ankush.home.domain.events.PlaceEventInput
import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.network.model.PlaceEventApiResult
import bose.ankush.network.model.PlaceEventSyncPage
import bose.ankush.storage.model.PlaceEventSyncState.FAILED
import bose.ankush.storage.model.PlaceEventSyncState.PENDING_DELETE
import bose.ankush.storage.model.PlaceEventSyncState.PENDING_UPSERT
import bose.ankush.storage.model.PlaceEventSyncState.SYNCED
import kotlinx.coroutines.flow.first
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
    fun aPost400MarksTheRowFailedWithTheServerMessageAndIsNotRetried() =
        runTest {
            val id = repository.createEvent(input).getOrThrow().id
            api.onCreate = { PlaceEventApiResult(400, message = "title is too long") }

            val report = engine.sync()

            assertEquals(FAILED, storage.row(id)?.syncState)
            assertEquals("title is too long", storage.row(id)?.syncError)
            assertEquals(PlaceEventSyncOutcome.COMPLETED, report.outcome)
            assertNull(report.retryAfter)
            assertTrue("GET since=null" in api.calls)

            api.calls.clear()
            engine.sync()

            assertTrue(api.calls.none { it.startsWith("POST") })
            assertEquals(FAILED, storage.row(id)?.syncState)
        }

    @Test
    fun aPost400WithoutAMessageGetsAGenericOne() =
        runTest {
            val id = repository.createEvent(input).getOrThrow().id
            api.onCreate = { PlaceEventApiResult(400) }

            engine.sync()

            assertEquals(REJECTED_MESSAGE, storage.row(id)?.syncError)
        }

    @Test
    fun aPut400MarksTheRowFailedKeepsItAndIsNotRetried() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_UPSERT, serverUpdatedAt = T_SERVER, title = "Mine"))
            api.onUpdate = { _, _ -> PlaceEventApiResult(400, message = "startsAt must be an ISO-8601 instant") }

            engine.sync()
            engine.sync()

            assertEquals(1, api.updated.size)
            assertEquals(FAILED, storage.row("a")?.syncState)
            assertEquals("Mine", storage.row("a")?.title)
            assertEquals("startsAt must be an ISO-8601 instant", storage.row("a")?.syncError)
        }

    @Test
    fun aRowEditedWhileItsRejectedPutWasInFlightStaysPending() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_UPSERT, serverUpdatedAt = T_SERVER))
            api.onUpdate = { _, _ ->
                repository.updateEvent("a", input.copy(title = "Fixed"))
                PlaceEventApiResult(400, message = "bad")
            }

            engine.sync()

            assertEquals(PENDING_UPSERT, storage.row("a")?.syncState)
            assertEquals("Fixed", storage.row("a")?.title)
            assertNull(storage.row("a")?.syncError)
        }

    @Test
    fun anEditRequeuesAFailedRowAndTheNextRunSendsIt() =
        runTest {
            val id = repository.createEvent(input).getOrThrow().id
            api.onCreate = { PlaceEventApiResult(400, message = "bad") }
            engine.sync()

            repository.updateEvent(id, input.copy(title = "Fixed")).getOrThrow()
            api.onCreate = { body -> PlaceEventApiResult(201, body.toDto(T_SERVER)) }
            engine.sync()

            assertEquals(listOf("Dinner", "Fixed"), api.created.map { it.title })
            assertEquals(SYNCED, storage.row(id)?.syncState)
            assertNull(storage.row(id)?.syncError)
        }

    @Test
    fun discardingAFailedEditFetchesTheServerCopyBackWithAFullResync() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_UPSERT, serverUpdatedAt = T_SERVER, title = "Mine"))
            storage.checkpoint = "2026-10-22T09:59:00.000Z~0123456789abcdef01234567"
            api.onUpdate = { _, _ -> PlaceEventApiResult(400, message = "bad") }
            engine.sync()
            assertEquals(FAILED, storage.row("a")?.syncState)

            repository.discardFailedEvent("a").getOrThrow()
            api.calls.clear()
            api.onChanges = { since -> page(since == null, listOf(serverDto("a", title = "Theirs"))) }
            engine.sync()

            assertEquals("GET since=null", api.calls.first())
            assertEquals("Theirs", storage.row("a")?.title)
            assertEquals(SYNCED, storage.row("a")?.syncState)
        }

    @Test
    fun aPullLeavesAFailedRowForTheUser() =
        runTest {
            storage.saveEvent(localRecord("a", FAILED, updatedAt = "2026-10-22T08:00:00Z").copy(syncError = "bad"))
            storage.saveEvent(localRecord("b", FAILED, serverUpdatedAt = T_SERVER).copy(syncError = "bad"))
            api.onChanges = { since ->
                page(since == null, listOf(serverDto("a"), serverDto("b", deletedAt = "2026-10-22T09:30:00.000Z")))
            }

            engine.sync()

            assertEquals(listOf("GET since=null"), api.calls.take(1))
            assertEquals("Local", storage.row("a")?.title)
            assertEquals(FAILED, storage.row("a")?.syncState)
            assertEquals(FAILED, storage.row("b")?.syncState)
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
    fun aPut409TakesTheCurrentServerCopyFromData() =
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
            storage.saveEvent(
                localRecord("a", PENDING_DELETE, updatedAt = "2026-10-22T10:00:01Z", serverUpdatedAt = T_SERVER),
            )
            storage.saveEvent(
                localRecord("b", PENDING_DELETE, updatedAt = "2026-10-22T10:00:02Z", serverUpdatedAt = T_SERVER),
            )
            api.onDelete = { id -> PlaceEventApiResult(if (id == "a") 204 else 404) }

            val report = engine.sync()

            assertEquals(listOf("DELETE a", "DELETE b"), api.calls.take(2))
            assertTrue(storage.rows.value.isEmpty())
            assertEquals(2, report.pushed)
        }

    @Test
    fun aSettledDeleteStaysGoneThroughThePullAndItsOverlap() =
        runTest {
            storage.saveEvent(localRecord("a", PENDING_DELETE, serverUpdatedAt = T_SERVER))
            // The server's tombstone shows up in both the pull and the overlap pull.
            val tombstone = serverDto("a", "2026-10-22T09:58:59.000Z", deletedAt = "2026-10-22T09:58:59.000Z")
            api.onChanges = { PlaceEventApiResult(200, PlaceEventSyncPage(listOf(tombstone), checkpoint = CHECKPOINT)) }

            engine.sync()

            assertEquals("DELETE a", api.calls.first())
            assertEquals(3, api.calls.size) // DELETE, the pull, the overlap pull
            assertNull(storage.row("a"))
        }

    @Test
    fun anEventCreatedAndDeletedOfflineNeverReachesTheServer() =
        runTest {
            api.online = false
            val id = repository.createEvent(input).getOrThrow().id
            repository.deleteEvent(id).getOrThrow()

            api.online = true
            engine.sync()

            assertNull(storage.row(id))
            assertTrue(api.created.isEmpty())
            assertTrue(api.calls.none { it.startsWith("POST") || it.startsWith("DELETE") || it.startsWith("PUT") })
        }

    @Test
    fun anEventDeletedWhileItsPostIsInFlightIsDeletedOnTheServerNext() =
        runTest {
            val id = repository.createEvent(input).getOrThrow().id
            api.onCreate = { body ->
                repository.deleteEvent(body.id).getOrThrow()
                PlaceEventApiResult(201, body.toDto(T_SERVER))
            }

            engine.sync()

            assertEquals(PENDING_DELETE, storage.row(id)?.syncState)
            assertTrue(repository.observeEvents(EventArea(input.point)).first().isEmpty())

            engine.sync()

            assertTrue("DELETE $id" in api.calls)
            assertNull(storage.row(id))
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
            api.onCreate = { PlaceEventApiResult(422) }
            api.onDelete = { PlaceEventApiResult(422) }
            api.onChanges = { since ->
                page(since == null, listOf(serverDto("newer"), serverDto("older"), serverDto("gone", title = "Back")))
            }

            engine.sync() // pushes answer 422 (kept pending); the pull still runs

            assertEquals("Mine", storage.row("newer")?.title)
            assertEquals(PENDING_UPSERT, storage.row("newer")?.syncState)
            assertEquals("Server", storage.row("older")?.title)
            assertEquals(SYNCED, storage.row("older")?.syncState)
            assertEquals(PENDING_DELETE, storage.row("gone")?.syncState)
        }

    @Test
    fun aPull400ForABadSinceResetsTheCheckpointAndResyncsInFullOnce() =
        runTest {
            storage.checkpoint = "not-a-cursor"
            api.onChanges = { since ->
                when (since) {
                    "not-a-cursor" -> PlaceEventApiResult(400, message = "since must be an ISO-8601 instant")
                    null ->
                        PlaceEventApiResult(
                            200,
                            PlaceEventSyncPage(listOf(serverDto("a")), checkpoint = CHECKPOINT),
                        )
                    else -> PlaceEventApiResult(200, PlaceEventSyncPage())
                }
            }

            val report = engine.sync()

            assertEquals(
                listOf("GET since=not-a-cursor", "GET since=null", "GET since=2026-10-22T09:58:50Z"),
                api.calls,
            )
            assertEquals(CHECKPOINT, storage.checkpoint)
            assertEquals(SYNCED, storage.row("a")?.syncState)
            assertEquals(PlaceEventSyncOutcome.COMPLETED, report.outcome)
        }

    @Test
    fun aPull400AfterTheResetStopsWithoutLooping() =
        runTest {
            storage.checkpoint = "not-a-cursor"
            api.onChanges = { PlaceEventApiResult(400) }

            engine.sync()

            assertEquals(listOf("GET since=not-a-cursor", "GET since=null"), api.calls)
            assertNull(storage.checkpoint)
        }

    @Test
    fun aPull5xxKeepsTheCheckpointAndBacksOff() =
        runTest {
            storage.checkpoint = CHECKPOINT
            api.onChanges = { PlaceEventApiResult(503) }

            val report = engine.sync()

            assertEquals(listOf("GET since=$CHECKPOINT"), api.calls)
            assertEquals(CHECKPOINT, storage.checkpoint)
            assertEquals(PlaceEventSyncOutcome.SERVER_ERROR, report.outcome)
            assertEquals(2.seconds, report.retryAfter)
        }

    @Test
    fun aPullThatGoesOfflineMidwayKeepsTheLastStoredCheckpoint() =
        runTest {
            storage.checkpoint = CHECKPOINT
            api.onChanges = { since ->
                if (since == CHECKPOINT) {
                    PlaceEventApiResult(200, PlaceEventSyncPage(nextSince = "c2", checkpoint = "c2", hasMore = true))
                } else {
                    throw FakeOfflineException()
                }
            }

            val report = engine.sync()

            assertEquals("c2", storage.checkpoint)
            assertEquals(PlaceEventSyncOutcome.OFFLINE, report.outcome)
            assertEquals(2.seconds, report.retryAfter)
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
                PlaceEventSyncPage(items, checkpoint = CHECKPOINT)
            } else {
                PlaceEventSyncPage(emptyList(), checkpoint = CHECKPOINT)
            },
        )

    private companion object {
        const val CHECKPOINT = "2026-10-22T09:59:00.000Z~0123456789abcdef01234567"
    }
}
