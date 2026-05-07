package bose.ankush.weatherify.domain.remote_config

/**
 * Service interface for handling remote configuration functionality.
 * This interface abstracts the underlying implementation details of remote configuration,
 * allowing for easier testing and flexibility in implementation.
 */
interface RemoteConfigService {
    /**
     * Initializes the remote configuration service.
     * This should be called early in the application lifecycle.
     */
    fun initialize()

    /**
     * Gets a boolean value from remote configuration.
     * @param key The key for the configuration value
     * @param defaultValue The default value to return if the key is not found
     * @return The boolean value from remote configuration, or the default value if not found
     */
    fun getBoolean(
        key: String,
        defaultValue: Boolean = false,
    ): Boolean
}
