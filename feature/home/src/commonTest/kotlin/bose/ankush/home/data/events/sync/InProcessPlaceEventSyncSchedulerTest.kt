package bose.ankush.home.data.events.sync

import bose.ankush.home.data.events.FakePlaceEventClock
import bose.ankush.home.data.events.FakePlaceEventStorage
import bose.ankush.home.data.events.FakePlaceEventSyncApi
import bose.ankush.home.data.events.PlaceEventWriteLock
import bose.ankush.network.model.PlaceEventApiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InProcessPlaceEventSyncSchedulerTest {
    private val api = FakePlaceEventSyncApi()
    private val engine =
        PlaceEventSyncEngine(
            FakePlaceEventStorage(),
            api,
            PlaceEventWriteLock(),
            PlaceEventSyncConfig(),
            FakePlaceEventClock(),
        )

    @Test
    fun nothingIsSentWhileSignedOut() =
        runTest {
            val scheduler = InProcessPlaceEventSyncScheduler(engine, backgroundScope, isSignedIn = { false })

            assertNull(scheduler.syncNow())
            scheduler.requestSync()
            runCurrent()

            assertTrue(api.calls.isEmpty())
        }

    @Test
    fun requestsAreConflatedAndARateLimitedRunRetriesAfterRetryAfter() =
        runTest {
            var calls = 0
            api.onChanges = {
                calls++
                if (calls == 1) PlaceEventApiResult(429, retryAfterSeconds = 30) else PlaceEventApiResult(200)
            }
            val scheduler =
                InProcessPlaceEventSyncScheduler(
                    engine,
                    kotlinx.coroutines.CoroutineScope(
                        StandardTestDispatcher(testScheduler),
                    ),
                    {
                        true
                    },
                )

            scheduler.requestSync()
            scheduler.requestSync()
            runCurrent()
            assertEquals(1, calls)

            advanceTimeBy(29_000)
            runCurrent()
            assertEquals(1, calls)

            advanceTimeBy(1_001)
            runCurrent()
            assertEquals(2, calls)
        }
}
