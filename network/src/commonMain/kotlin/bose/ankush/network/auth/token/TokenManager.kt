package bose.ankush.network.auth.token

import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.storage.api.TokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class TokenManager(
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
) {
    private val refreshMutex = Mutex()
    private var lastRefreshTime: Long = 0

    suspend fun refreshToken(currentTime: Long = Clock.System.now().epochSeconds): TokenResult =
        refreshMutex.withLock {
            lastRefreshTime = currentTime
            try {
                val response =
                    authRepository.refreshToken()
                        ?: return TokenResult.NoToken
                val newToken = response.data?.token
                if (response.isSuccess() && !newToken.isNullOrBlank()) {
                    tokenStorage.saveToken(newToken)
                    return TokenResult.Valid(newToken)
                }
                // If token is missing in a successful response, treat as no token
                if (response.isSuccess() && newToken.isNullOrBlank()) {
                    return TokenResult.NoToken
                }
                return TokenResult.InvalidToken(response.data?.errorCode)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                return TokenResult.Error(e)
            }
        }

    /**
     * Returns the currently stored token without attempting a refresh.
     * Used by the auth interceptor to attach a token to every outgoing request.
     */
    suspend fun getStoredToken(): String? = tokenStorage.getToken()

    suspend fun handleUnauthorized(): TokenResult {
        lastRefreshTime = 0
        return refreshToken()
    }

    suspend fun forceLogout(): TokenResult =
        try {
            tokenStorage.clearToken()
            TokenResult.NoToken
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            TokenResult.Error(e)
        }
}
