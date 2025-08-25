package bose.ankush.storage.impl

import bose.ankush.network.auth.storage.TokenStorage
import bose.ankush.storage.room.AuthToken
import bose.ankush.storage.room.WeatherDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Android implementation of TokenStorage using Room database
 */
class TokenStorageImpl(
    private val database: WeatherDatabase
) : TokenStorage {

    override suspend fun saveToken(token: String) {
        withContext(Dispatchers.IO) {
            val authToken = AuthToken(token = token)
            database.authTokenDao().saveToken(authToken)
        }
    }

    override suspend fun getToken(): String? {
        return withContext(Dispatchers.IO) {
            database.authTokenDao().getToken()?.token
        }
    }

    override fun hasToken(): Flow<Boolean> {
        // Flow is already asynchronous, so we don't need to use withContext here
        return database.authTokenDao().hasToken()
    }

    override suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            database.authTokenDao().clearTokens()
        }
    }
}