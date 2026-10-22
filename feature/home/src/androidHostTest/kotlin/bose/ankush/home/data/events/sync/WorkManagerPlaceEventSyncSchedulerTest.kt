package bose.ankush.home.data.events.sync

import bose.ankush.home.data.events.FakePlaceEventStorage
import bose.ankush.home.data.events.FakePlaceEventSyncApi
import bose.ankush.home.data.events.PlaceEventWriteLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Session hooks and triggers of [WorkManagerPlaceEventSyncScheduler], against a recording queue. */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkManagerPlaceEventSyncSchedulerTest {
    private val queue = RecordingQueue()
    private val session = MutableStateFlow(false)
    private val api = FakePlaceEventSyncApi()

    private fun TestScope.scheduler(signedIn: Boolean = session.value) =
        WorkManagerPlaceEventSyncScheduler(
            queue = queue,
            engine = PlaceEventSyncEngine(FakePlaceEventStorage(), api, PlaceEventWriteLock()),
            isSignedIn = { signedIn },
            signedInChanges = session,
            scope = backgroundScope.plusUnconfined(this),
        )

    @Test
    fun signingInStartsBothJobsAndSigningOutCancelsThem() =
        runTest {
            scheduler()
            assertEquals(listOf("cancel"), queue.ops) // signed out at start: nothing should run

            session.value = true
            assertEquals(listOf("cancel", "periodic", "now"), queue.ops)

            session.value = false
            assertEquals(listOf("cancel", "periodic", "now", "cancel"), queue.ops)
        }

    @Test
    fun aSessionAlreadySignedInAtStartEnqueuesOnce() =
        runTest {
            session.value = true
            scheduler()

            assertEquals(listOf("periodic", "now"), queue.ops)
        }

    @Test
    fun aWriteEnqueuesTheOneTimeJobNow() =
        runTest {
            session.value = true
            val scheduler = scheduler()
            queue.ops.clear()

            scheduler.requestSync()

            assertEquals(listOf("now"), queue.ops)
        }

    @Test
    fun aLongRetryAfterIsEnqueuedWithItsDelay() =
        runTest {
            val scheduler = scheduler()
            queue.ops.clear()

            scheduler.retryLater(120.seconds)

            assertEquals(listOf("now+2m"), queue.ops)
        }

    @Test
    fun nothingIsSyncedWhileSignedOut() =
        runTest {
            assertNull(scheduler(signedIn = false).syncNow())
            assertEquals(emptyList(), api.calls)
        }

    private fun kotlinx.coroutines.CoroutineScope.plusUnconfined(test: TestScope) =
        kotlinx.coroutines.CoroutineScope(coroutineContext + UnconfinedTestDispatcher(test.testScheduler))

    private class RecordingQueue : PlaceEventSyncWorkQueue {
        val ops = mutableListOf<String>()

        override fun enqueuePeriodic() {
            ops += "periodic"
        }

        override fun enqueueNow(delay: Duration?) {
            ops += if (delay == null) "now" else "now+$delay"
        }

        override fun cancelAll() {
            ops += "cancel"
        }
    }
}
