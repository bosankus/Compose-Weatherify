package bose.ankush.storage

import bose.ankush.tokenstorage.api.TokenStorage
import kotlinx.coroutines.flow.Flow

expect class EncryptedTokenStorageImpl : TokenStorage {
    override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        tokenType: String?,
    )

    override suspend fun getToken(): String?

    override suspend fun getRefreshToken(): String?

    override suspend fun getTokenType(): String?

    override fun hasToken(): Flow<Boolean>

    override suspend fun clearToken()
}
