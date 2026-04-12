package bose.ankush.storage.api

import kotlinx.coroutines.flow.Flow

/**
 * Interface for secure token storage
 * This will be implemented differently on each platform
 */
interface TokenStorage {
    /**
     * Save a token
     * @param token The JWT token to save
     */
    suspend fun saveToken(token: String)

    /**
     * Get the stored token
     * @return The JWT token or null if not available
     */
    suspend fun getToken(): String?

    /**
     * Check if a token exists
     * @return Flow of Boolean indicating if a token exists
     */
    fun hasToken(): Flow<Boolean>

    /**
     * Clear the stored token
     */
    suspend fun clearToken()
}
