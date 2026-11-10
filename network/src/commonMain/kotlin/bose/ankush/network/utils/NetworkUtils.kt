package bose.ankush.network.utils

import bose.ankush.network.util.NetworkException
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

object NetworkUtils {
    suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = NetworkConstants.MAX_RETRIES,
        initialDelayMillis: Long = NetworkConstants.INITIAL_BACKOFF_DELAY,
        maxDelayMillis: Long = NetworkConstants.MAX_BACKOFF_DELAY,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelayMillis
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: CancellationException) {
                // Not a failed request: wrapping it would turn a cancelled call into an error.
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt == maxRetries - 1) {
                    if (e is NetworkException) throw e else throw NetworkException.fromException(e)
                }
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMillis)
            }
        }
        // This should never be reached, but is needed for compilation
        throw NetworkException(
            NetworkException.UNKNOWN_ERROR,
            "Retry failed after $maxRetries attempts",
            lastException,
        )
    }
}
