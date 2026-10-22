package bose.ankush.home.data.events

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serialises writes to the place-event store between user edits and the sync engine, so a sync
 * result never lands on a row the user changed in the meantime without noticing.
 *
 * [generation] moves on every [clear] (sign-out). A sync run captures it at the start and its
 * writes are dropped once it changed, so a run still in flight cannot bring back the previous
 * account's events.
 */
internal class PlaceEventWriteLock {
    private val mutex = Mutex()
    private var generation = 0L

    suspend fun currentGeneration(): Long = mutex.withLock { generation }

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }

    /** Runs [block] only if nothing was cleared since [expected]; false when it was skipped. */
    suspend fun withLockIfCurrent(
        expected: Long,
        block: suspend () -> Unit,
    ): Boolean =
        mutex.withLock {
            val current = generation == expected
            if (current) block()
            current
        }

    suspend fun clear(block: suspend () -> Unit) =
        mutex.withLock {
            generation++
            block()
        }
}
