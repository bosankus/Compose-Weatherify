package bose.ankush.network.utils

/**
 * Network-specific constants for the network module
 */
object NetworkConstants {
    /**
     * Base URL for the weather API
     */
    const val WEATHER_BASE_URL = "https://data.androidplay.in"

    /**
     * Cache expiration time in milliseconds (30 minutes)
     */
    const val CACHE_EXPIRATION_TIME = 30 * 60 * 1000L

    /**
     * Maximum number of retries for network requests
     */
    const val MAX_RETRIES = 3

    /**
     * Initial backoff delay in milliseconds for retry mechanism
     */
    const val INITIAL_BACKOFF_DELAY = 1000L

    /**
     * Maximum backoff delay in milliseconds for retry mechanism
     */
    const val MAX_BACKOFF_DELAY = 30000L
}
