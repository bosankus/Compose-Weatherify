package bose.ankush.storage.impl

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import bose.ankush.storage.api.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * SECURITY: Encrypted token storage using EncryptedSharedPreferences with Android Keystore
 *
 * This implementation uses androidx.security:security-crypto to encrypt tokens at rest
 * using the Android Keystore system. Tokens are encrypted/decrypted transparently.
 *
 * Benefits:
 * - Tokens encrypted with AES-256-GCM
 * - Keys managed by Android Keystore (hardware-backed on supported devices)
 * - Protection against database extraction attacks
 * - Complies with OWASP guidelines for credential storage
 */
actual class EncryptedTokenStorageImpl : TokenStorage {

    private val context: Context by lazy {
        getApplicationContext()
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _hasToken by lazy {
        MutableStateFlow(encryptedSharedPreferences.contains(TOKEN_KEY))
    }

    actual override suspend fun saveToken(token: String) {
        withContext(Dispatchers.IO) {
            @Suppress("ApplySharedPref")
            encryptedSharedPreferences.edit().putString(TOKEN_KEY, token).apply()
        }
        _hasToken.value = true
    }

    actual override suspend fun getToken(): String? {
        return withContext(Dispatchers.IO) {
            encryptedSharedPreferences.getString(TOKEN_KEY, null)
        }
    }

    actual override fun hasToken(): Flow<Boolean> = _hasToken.asStateFlow()

    actual override suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            @Suppress("ApplySharedPref")
            encryptedSharedPreferences.edit().remove(TOKEN_KEY).apply()
        }
        _hasToken.value = false
    }

    companion object {
        private const val PREFS_NAME = "encrypted_auth_prefs"
        private const val TOKEN_KEY = "auth_token"
    }
}

private var appContext: Context? = null

fun setApplicationContext(context: Context) {
    appContext = context
}

private fun getApplicationContext(): Context {
    return appContext ?: throw IllegalStateException(
        "Application context not initialized. Call setApplicationContext() in your Application.onCreate()"
    )
}
