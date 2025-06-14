package bose.ankush.network.common

import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * Utility functions for network operations
 */
object NetworkUtils {
    /**
     * Retry a network request with exponential backoff
     * @param maxRetries Maximum number of retries
     * @param initialDelayMillis Initial delay in milliseconds
     * @param block The suspend function to retry
     * @return The result of the suspend function
     * @throws Exception if all retries fail
     */
    suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = Constants.MAX_RETRIES,
        initialDelayMillis: Long = Constants.INITIAL_BACKOFF_DELAY,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMillis
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                // If this is the last attempt, throw the exception
                if (attempt == maxRetries - 1) throw e

                // Otherwise, delay and retry
                delay(currentDelay)
                currentDelay = (currentDelay * 2.0.pow(attempt)).toLong()
            }
        }
        // This should never be reached, but is needed for compilation
        throw IllegalStateException("Retry failed after $maxRetries attempts")
    }
}