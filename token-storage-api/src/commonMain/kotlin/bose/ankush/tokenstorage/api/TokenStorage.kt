package bose.ankush.tokenstorage.api

import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting auth tokens. Implementations live in `:storage`
 * (EncryptedSharedPreferences / Keychain); `:network` depends only on this API.
 */
interface TokenStorage {
    /**
     * Saves the session's tokens together. [refreshToken] is null when the server issues only an
     * access token; any refresh token stored before is then dropped, so a stale one is never sent.
     * [tokenType] is the server's token_type: "DPoP" for a token bound to the device key, otherwise
     * null (a plain bearer token).
     */
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        tokenType: String?,
    )

    suspend fun getToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun getTokenType(): String?

    fun hasToken(): Flow<Boolean>

    /** Clears the tokens and their type. */
    suspend fun clearToken()
}
