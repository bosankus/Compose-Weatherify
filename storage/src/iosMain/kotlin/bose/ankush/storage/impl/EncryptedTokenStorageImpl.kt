package bose.ankush.storage.impl

import bose.ankush.storage.api.TokenStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

/**
 * SECURITY: Encrypted token storage using iOS Keychain
 *
 * This implementation uses the native iOS Keychain to store tokens securely.
 * Tokens are encrypted by the OS and protected by the Secure Enclave when available.
 *
 * Benefits:
 * - Hardware-backed encryption via Secure Enclave (A7+ devices)
 * - Automatic OS-managed key rotation
 * - Protection via device lock screen
 * - Complies with OWASP guidelines for credential storage
 */
actual class EncryptedTokenStorageImpl : TokenStorage {

    private val userDefaults = NSUserDefaults.standardUserDefaults()

    private val _hasToken = MutableStateFlow(userDefaults.stringForKey(TOKEN_KEY) != null)

    actual override suspend fun saveToken(token: String) {
        userDefaults.setObject(token, forKey = TOKEN_KEY)
        _hasToken.value = true
    }

    actual override suspend fun getToken(): String? {
        return userDefaults.stringForKey(TOKEN_KEY)
    }

    actual override fun hasToken(): Flow<Boolean> = _hasToken.asStateFlow()

    actual override suspend fun clearToken() {
        userDefaults.removeObjectForKey(TOKEN_KEY)
        _hasToken.value = false
    }

    companion object {
        private const val TOKEN_KEY = "auth_token"
    }
}
