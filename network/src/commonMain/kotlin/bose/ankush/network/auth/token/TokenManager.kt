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
    private val authRepository: AuthRepository
) {
    private val refreshMutex = Mutex()
    private var lastRefreshTime: Long = 0
    private val minRefreshInterval = 5 * 60L

    /**
     * Returns the current valid token, refreshing if necessary.
     */
    suspend fun getValidToken(forceRefresh: Boolean = false): TokenResult {
        val currentToken = tokenStorage.getToken()
            ?: return TokenResult.NoToken
        val now = Clock.System.now().epochSeconds
        if (forceRefresh || canAttemptRefresh(now)) {
            val result = refreshToken(now)
            if (result.isValid()) return result
            // On error, propagate it instead of falling back silently
            if (result is TokenResult.Error) return result
            if (result is TokenResult.InvalidToken) return result
        }
        return TokenResult.Valid(currentToken)
    }

    private fun canAttemptRefresh(currentTime: Long): Boolean {
        return (currentTime - lastRefreshTime) >= minRefreshInterval
    }

    /**
     * Refreshes the token if possible.
     */
    suspend fun refreshToken(currentTime: Long = Clock.System.now().epochSeconds): TokenResult =
        refreshMutex.withLock {
            lastRefreshTime = currentTime
            try {
                val response = authRepository.refreshToken()
                    ?: return TokenResult.Error(IllegalStateException("Refresh returned null response"))
                val newToken = response.data?.token
                if (response.isSuccess() && !newToken.isNullOrBlank()) {
                    tokenStorage.saveToken(newToken)
                    return TokenResult.Valid(newToken)
                }
                when (response.data?.errorCode) {
                    "TOKEN_NOT_EXPIRED" -> {
                        val existingToken = tokenStorage.getToken()
                        return if (existingToken != null) TokenResult.Valid(existingToken)
                        else TokenResult.NoToken
                    }

                    "TOKEN_INVALID" -> {
                        return TokenResult.InvalidToken(response.data.errorCode)
                    }

                    else -> {
                        return TokenResult.InvalidToken(response.data?.errorCode)
                    }
                }
            } catch (e: Exception) {
                return TokenResult.Error(e)
            }
        }

    /**
     * Handles 401 Unauthorized by forcing a token refresh.
     */
    suspend fun handleUnauthorized(): TokenResult {
        lastRefreshTime = 0
        return refreshToken()
    }

    /**
     * Forces logout by clearing any stored token.
     */
    suspend fun forceLogout(): TokenResult {
        return try {
            tokenStorage.clearToken()
            TokenResult.NoToken
        } catch (e: Exception) {
            TokenResult.Error(e)
        }
    }
}
