package bose.ankush.storage

import bose.ankush.storage.api.TokenStorage
import kotlinx.coroutines.flow.Flow

expect class EncryptedTokenStorageImpl : TokenStorage {
    override suspend fun saveToken(token: String)

    override suspend fun getToken(): String?

    override fun hasToken(): Flow<Boolean>

    override suspend fun clearToken()
}