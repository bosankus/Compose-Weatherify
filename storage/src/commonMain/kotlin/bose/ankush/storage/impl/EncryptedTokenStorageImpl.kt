package bose.ankush.storage.impl

import bose.ankush.storage.api.TokenStorage
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific encrypted token storage factory
 *
 * Each platform provides its own implementation:
 * - Android: Uses EncryptedSharedPreferences with Android Keystore
 * - iOS: Uses Keychain for secure token storage
 */
expect class EncryptedTokenStorageImpl : TokenStorage {
    override suspend fun saveToken(token: String)
    override suspend fun getToken(): String?
    override fun hasToken(): Flow<Boolean>
    override suspend fun clearToken()
}
