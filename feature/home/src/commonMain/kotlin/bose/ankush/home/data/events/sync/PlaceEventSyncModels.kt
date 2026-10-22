package bose.ankush.home.data.events.sync

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Limits for one sync run. The server allows 60 requests a minute per account across every
 * endpoint, so a run stops at [maxRequestsPerRun] and leaves the rest pending.
 */
internal data class PlaceEventSyncConfig(
    val maxRequestsPerRun: Int = 40,
    /** The extra pull after the last page starts this far before the checkpoint. */
    val overlap: Duration = 10.seconds,
    /** First wait after a 429 without Retry-After, or after an offline / 5xx run; doubles each time. */
    val baseBackoff: Duration = 2.seconds,
    val maxBackoff: Duration = 5.minutes,
    /** After a capped run the next one waits for the rate window to roll. */
    val capCooldown: Duration = 1.minutes,
    /** Consecutive failed runs retried automatically before waiting for the next trigger. */
    val maxAutoRetries: Int = 6,
)

/** Why a run ended. */
internal enum class PlaceEventSyncOutcome {
    /** Pushed what it could and pulled to the end, overlap pull included. */
    COMPLETED,

    /** Hit [PlaceEventSyncConfig.maxRequestsPerRun]; the rest stays pending. */
    REQUEST_CAP,

    /** 429 from the server. */
    RATE_LIMITED,

    /** A request did not reach the server (offline, timeout). */
    OFFLINE,

    /** A 5xx from the server. */
    SERVER_ERROR,

    /** 401/403 after the token refresh; nothing more to do until the user signs in. */
    UNAUTHORIZED,

    /** The store was cleared (sign-out) while the run was in flight; its results were dropped. */
    CLEARED,
}

/**
 * What a run did. [retryAfter] is when the scheduler should try again by itself, or null when
 * it should wait for the next trigger (create/edit/delete, app start, foreground).
 * [serverRetryAfter] is the 429's `Retry-After`, when the server sent one.
 */
internal data class PlaceEventSyncReport(
    val outcome: PlaceEventSyncOutcome,
    val requests: Int,
    val pushed: Int,
    val pulled: Int,
    val retryAfter: Duration? = null,
    val serverRetryAfter: Duration? = null,
)
