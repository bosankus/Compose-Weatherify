package bose.ankush.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import bose.ankush.tokenstorage.api.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * SECURITY: Tokens encrypted at rest using AES-256-GCM via Android Keystore
 * (hardware-backed on supported devices). Complies with OWASP credential storage guidelines.
 */
actual class EncryptedTokenStorageImpl : TokenStorage {
    private val context: Context by lazy {
        getApplicationContext()
    }

    private val masterKey: MasterKey by lazy {
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _hasToken by lazy {
        MutableStateFlow(encryptedSharedPreferences.contains(TOKEN_KEY))
    }

    actual override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        tokenType: String?,
    ) {
        withContext(Dispatchers.IO) {
            // One commit, so the tokens and their type can never end up from different sessions.
            val success =
                encryptedSharedPreferences
                    .edit()
                    .putString(TOKEN_KEY, accessToken)
                    .apply {
                        if (refreshToken != null) {
                            putString(REFRESH_TOKEN_KEY, refreshToken)
                        } else {
                            remove(REFRESH_TOKEN_KEY)
                        }
                        if (tokenType != null) {
                            putString(TOKEN_TYPE_KEY, tokenType)
                        } else {
                            remove(TOKEN_TYPE_KEY)
                        }
                    }.commit()
            if (success) {
                _hasToken.value = true
            }
        }
    }

    actual override suspend fun getToken(): String? =
        withContext(Dispatchers.IO) {
            encryptedSharedPreferences.getString(TOKEN_KEY, null)
        }

    actual override suspend fun getRefreshToken(): String? =
        withContext(Dispatchers.IO) {
            encryptedSharedPreferences.getString(REFRESH_TOKEN_KEY, null)
        }

    actual override suspend fun getTokenType(): String? =
        withContext(Dispatchers.IO) {
            encryptedSharedPreferences.getString(TOKEN_TYPE_KEY, null)
        }

    actual override fun hasToken(): Flow<Boolean> = _hasToken.asStateFlow()

    actual override suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            val success =
                encryptedSharedPreferences
                    .edit()
                    .remove(TOKEN_KEY)
                    .remove(REFRESH_TOKEN_KEY)
                    .remove(TOKEN_TYPE_KEY)
                    .commit()
            if (success) {
                _hasToken.value = false
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "encrypted_auth_prefs"
        private const val TOKEN_KEY = "auth_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_TYPE_KEY = "token_type"
    }
}

private var appContext: Context? = null

fun setApplicationContext(context: Context) {
    appContext = context
}

private fun getApplicationContext(): Context =
    appContext ?: throw IllegalStateException(
        "Application context not initialized. Call setApplicationContext() in your Application.onCreate()",
    )
