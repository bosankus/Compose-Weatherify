package bose.ankush.network.auth.token

import bose.ankush.network.auth.dpop.authorizationScheme
import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.tokenstorage.api.TokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The one way to refresh the session. Every refresh runs under one lock, so a refresh token is
 * presented once and never again: with rotation, the server treats a second use as theft and
 * revokes the session.
 */
class TokenManager(
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
) {
    private val refreshMutex = Mutex()

    /** Refreshes now and returns the server's response, for callers that need its data (premium). */
    suspend fun refreshSession(): AuthResponse? =
        refreshMutex.withLock { authRepository.refreshToken() }

    /**
     * Returns the currently stored token without attempting a refresh.
     * Used by the auth interceptor to attach a token to every outgoing request.
     */
    suspend fun getStoredToken(): String? = tokenStorage.getToken()

    /** "DPoP" once the server has bound the stored token to the device key, otherwise "Bearer". */
    suspend fun authorizationScheme(): String = authorizationScheme(tokenStorage.getTokenType())

    /**
     * The server refused [rejectedToken]. When requests fail together, the first to get the lock
     * refreshes and the rest find a newer token already stored and reuse it.
     */
    suspend fun handleUnauthorized(rejectedToken: String?): TokenResult =
        refreshMutex.withLock {
            val current = tokenStorage.getToken()
            if (current != null && current != rejectedToken) TokenResult.Valid(current) else exchange()
        }

    private suspend fun exchange(): TokenResult =
        try {
            val response = authRepository.refreshToken()
            val newToken = response?.data?.token
            when {
                response == null -> TokenResult.NoToken
                response.isSuccess() && !newToken.isNullOrBlank() -> TokenResult.Valid(newToken)
                // A success without a token can't authenticate anything.
                response.isSuccess() -> TokenResult.NoToken
                else -> TokenResult.InvalidToken(response.data?.errorCode)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            TokenResult.Error(e)
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
