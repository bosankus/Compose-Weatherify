package bose.ankush.network.auth.token

import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.storage.api.TokenStorage
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
     * Returns the currently stored token without attempting a refresh.
     * Used by the auth interceptor to attach a token to every outgoing request.
     */
    suspend fun getStoredToken(): String? = tokenStorage.getToken()

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
