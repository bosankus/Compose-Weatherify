package bose.ankush.tokenstorage.api

import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting auth tokens. Implementations live in `:storage`
 * (EncryptedSharedPreferences / Keychain); `:network` depends only on this API.
 */
interface TokenStorage {
    suspend fun saveToken(token: String)

    suspend fun getToken(): String?

    fun hasToken(): Flow<Boolean>

    suspend fun clearToken()
}
