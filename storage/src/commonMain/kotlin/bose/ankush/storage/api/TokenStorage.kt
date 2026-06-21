package bose.ankush.storage.api

import kotlinx.coroutines.flow.Flow

interface TokenStorage {
    suspend fun saveToken(token: String)

    suspend fun getToken(): String?

    fun hasToken(): Flow<Boolean>

    suspend fun clearToken()
}
