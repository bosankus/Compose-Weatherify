package bose.ankush.home.data.events.sync

import bose.ankush.home.data.events.PlaceEventWriteLock
import bose.ankush.network.model.PlaceEventApiResult
import kotlinx.coroutines.CancellationException

/**
 * State of one sync run: the request budget, why it stopped, and the store generation it
 * started on. Every request and every store write in a run goes through here.
 */
internal class SyncRun(
    private val maxRequests: Int,
    private val lock: PlaceEventWriteLock,
    private val generation: Long,
) {
    var requests = 0
        private set
    var stopReason: PlaceEventSyncOutcome? = null
        private set
    var retryAfterSeconds: Long? = null
        private set

    val isStopped: Boolean get() = stopReason != null

    /**
     * Sends one request if the run may still send. Returns null, and stops the run, on the
     * request cap, a transport failure, 401/403, 429 or 5xx; any other status is the caller's.
     */
    suspend fun <T> call(block: suspend () -> PlaceEventApiResult<T>): PlaceEventApiResult<T>? {
        if (stopReason == null && requests >= maxRequests) stopReason = PlaceEventSyncOutcome.REQUEST_CAP
        if (stopReason != null) return null
        requests++
        val result = attempt(block)
        val stop = if (result == null) PlaceEventSyncOutcome.OFFLINE else stopFor(result.statusCode)
        if (stop != null) {
            stopReason = stop
            retryAfterSeconds = result?.retryAfterSeconds
        }
        return result.takeIf { stop == null }
    }

    /** Writes under the store lock unless the store was cleared since the run began. */
    suspend fun write(block: suspend () -> Unit) {
        if (isStopped && stopReason == PlaceEventSyncOutcome.CLEARED) return
        if (!lock.withLockIfCurrent(generation, block)) stopReason = PlaceEventSyncOutcome.CLEARED
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> attempt(block: suspend () -> PlaceEventApiResult<T>): PlaceEventApiResult<T>? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    private fun stopFor(status: Int): PlaceEventSyncOutcome? =
        when (status) {
            HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> PlaceEventSyncOutcome.UNAUTHORIZED
            HTTP_TOO_MANY_REQUESTS -> PlaceEventSyncOutcome.RATE_LIMITED
            in HTTP_SERVER_ERRORS -> PlaceEventSyncOutcome.SERVER_ERROR
            else -> null
        }
}

internal const val HTTP_OK = 200
internal const val HTTP_CREATED = 201
internal const val HTTP_NO_CONTENT = 204
internal const val HTTP_BAD_REQUEST = 400
internal const val HTTP_NOT_FOUND = 404
internal const val HTTP_CONFLICT = 409
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_TOO_MANY_REQUESTS = 429
private val HTTP_SERVER_ERRORS = 500..599
