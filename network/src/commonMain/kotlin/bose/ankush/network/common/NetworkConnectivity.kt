package bose.ankush.network.common

/**
 * Interface for checking network connectivity
 * This will be implemented differently on each platform (Android, iOS)
 */
interface NetworkConnectivity {
    /**
     * Check if network is available
     * @return true if network is available, false otherwise
     */
    fun isNetworkAvailable(): Boolean
}