package bose.ankush.network.auth.token

import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.storage.TokenStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Handles JWT token lifecycle including validation and refresh.
 */
class TokenManager(
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
    private val debugLogging: Boolean = true
) {
    private val refreshMutex = Mutex()
    private var lastRefreshTime: Long = 0
    private val minRefreshInterval = 5 * 60L

    /**
     * Returns the current valid token, refreshing if necessary.
     */
    suspend fun getValidToken(forceRefresh: Boolean = false): String? {
        val currentToken = tokenStorage.getToken() ?: return null
        val now = Clock.System.now().epochSeconds
        if (forceRefresh || canAttemptRefresh(now)) {
            val refreshedToken = refreshToken(now)
            if (refreshedToken != null) return refreshedToken
        }
        return currentToken
    }

    private fun canAttemptRefresh(currentTime: Long): Boolean {
        return (currentTime - lastRefreshTime) >= minRefreshInterval
    }

    /**
     * Refreshes the token if possible.
     */
    suspend fun refreshToken(currentTime: Long = Clock.System.now().epochSeconds): String? =
        refreshMutex.withLock {
            lastRefreshTime = currentTime
            try {
                val response = authRepository.refreshToken() ?: return null
                val newToken = response.data?.token
                if (response.status && !newToken.isNullOrBlank()) {
                    tokenStorage.saveToken(newToken)
                    if (debugLogging) println("[DEBUG_LOG] Token refreshed successfully")
                    return newToken
                } else {
                    if (debugLogging) println("[DEBUG_LOG] Token refresh failed: Invalid response")
                }
            } catch (e: Exception) {
                if (debugLogging) println("[DEBUG_LOG] Token refresh failed: ${e.message}")
            }
            return null
        }

    /**
     * Handles 401 Unauthorized by forcing a token refresh.
     */
    suspend fun handleUnauthorized(): Boolean {
        lastRefreshTime = 0
        return refreshToken() != null
    }
}