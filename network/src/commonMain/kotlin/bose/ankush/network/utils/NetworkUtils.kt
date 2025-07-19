package bose.ankush.network.utils

import bose.ankush.network.common.NetworkException
import kotlinx.coroutines.delay

/**
 * Utility functions for network operations
 */
object NetworkUtils {
    /**
     * Retry a network request with exponential backoff
     * @param maxRetries Maximum number of retries
     * @param initialDelayMillis Initial delay in milliseconds
     * @param maxDelayMillis Maximum delay in milliseconds
     * @param block The suspend function to retry
     * @return The result of the suspend function
     * @throws NetworkException if all retries fail
     */
    suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = NetworkConstants.MAX_RETRIES,
        initialDelayMillis: Long = NetworkConstants.INITIAL_BACKOFF_DELAY,
        maxDelayMillis: Long = NetworkConstants.MAX_BACKOFF_DELAY,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMillis
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e

                // If this is the last attempt, convert to NetworkException and throw
                if (attempt == maxRetries - 1) {
                    if (e is NetworkException) {
                        throw e
                    } else {
                        throw NetworkException.fromException(e)
                    }
                }

                // Otherwise, delay and retry
                delay(currentDelay)
                // Simply double the delay for each retry, but cap it at maxDelayMillis
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMillis)
            }
        }
        // This should never be reached, but is needed for compilation
        throw NetworkException(
            NetworkException.UNKNOWN_ERROR,
            "Retry failed after $maxRetries attempts",
            lastException
        )
    }
}
