package bose.ankush.storage

import bose.ankush.tokenstorage.api.TokenStorage
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * SECURITY: Encrypted token storage using iOS Keychain with Secure Enclave support.
 *
 * Tokens are encrypted by the OS, hardware-backed via Secure Enclave (A7+), protected
 * by device lock, and stored with kSecAttrAccessibleWhenUnlockedThisDeviceOnly so they
 * are never synced to iCloud.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class EncryptedTokenStorageImpl : TokenStorage {
    private val hasTokenState = MutableStateFlow(false)

    init {
        hasTokenState.value = retrieveFromKeychain(ACCOUNT_ID) != null
    }

    actual override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        tokenType: String?,
    ) {
        // Refresh token first: if the access token write then fails, the session still refreshes
        // with the newest refresh token instead of presenting a rotated-out one.
        if (refreshToken != null) {
            writeToKeychain(REFRESH_ACCOUNT_ID, refreshToken)
        } else {
            deleteFromKeychain(REFRESH_ACCOUNT_ID)
        }
        if (tokenType != null) {
            writeToKeychain(TOKEN_TYPE_ACCOUNT_ID, tokenType)
        } else {
            deleteFromKeychain(TOKEN_TYPE_ACCOUNT_ID)
        }
        writeToKeychain(ACCOUNT_ID, accessToken)
        hasTokenState.value = true
    }

    actual override suspend fun getToken(): String? = retrieveFromKeychain(ACCOUNT_ID)

    actual override suspend fun getRefreshToken(): String? =
        retrieveFromKeychain(REFRESH_ACCOUNT_ID)

    actual override suspend fun getTokenType(): String? =
        retrieveFromKeychain(TOKEN_TYPE_ACCOUNT_ID)

    actual override fun hasToken(): Flow<Boolean> = hasTokenState.asStateFlow()

    actual override suspend fun clearToken() {
        deleteFromKeychain(REFRESH_ACCOUNT_ID)
        deleteFromKeychain(TOKEN_TYPE_ACCOUNT_ID)
        deleteFromKeychain(ACCOUNT_ID)
        hasTokenState.value = false
    }

    private fun writeToKeychain(
        account: String,
        value: String,
    ) {
        val data =
            NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)
                ?: throw Exception("Failed to encode token to NSData")

        deleteFromKeychain(account)

        val query = buildBaseQuery(account)
        CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(data))

        val status = SecItemAdd(query, null)
        if (status != 0) {
            throw Exception("Failed to save token to Keychain: error code $status")
        }
    }

    private fun retrieveFromKeychain(account: String): String? {
        val query = buildBaseQuery(account)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        return memScoped {
            val resultRef = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, resultRef.ptr)
            if (status == 0) {
                val nsData = CFBridgingRelease(resultRef.value) as? NSData
                nsData?.let {
                    NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
                }
            } else {
                null
            }
        }
    }

    private fun deleteFromKeychain(account: String) {
        val query = buildBaseQuery(account)
        val status = SecItemDelete(query)
        // errSecItemNotFound (-25300) is acceptable — nothing to delete
        if (status != 0 && status != -25300) {
            throw Exception("Failed to delete token from Keychain: error code $status")
        }
    }

    private fun buildBaseQuery(account: String): CFMutableDictionaryRef {
        val dict =
            CFDictionaryCreateMutable(
                null,
                4,
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            ) ?: throw Exception("Failed to create CFMutableDictionary")
        CFDictionaryAddValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(dict, kSecAttrService, CFBridgingRetain(SERVICE_ID as NSString))
        CFDictionaryAddValue(dict, kSecAttrAccount, CFBridgingRetain(account as NSString))
        CFDictionaryAddValue(dict, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
        return dict
    }

    companion object {
        private const val SERVICE_ID = "com.weatherify.auth"
        private const val ACCOUNT_ID = "auth_token"
        private const val REFRESH_ACCOUNT_ID = "refresh_token"
        private const val TOKEN_TYPE_ACCOUNT_ID = "token_type"
    }
}
