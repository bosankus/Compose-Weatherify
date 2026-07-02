@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")

package bose.ankush.storage

import bose.ankush.storage.api.TokenStorage
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
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
        hasTokenState.value = retrieveTokenFromKeychain() != null
    }

    actual override suspend fun saveToken(token: String) {
        val tokenData =
            NSString.create(string = token).dataUsingEncoding(NSUTF8StringEncoding)
                ?: throw Exception("Failed to encode token to NSData")

        deleteTokenFromKeychain()

        val query = buildBaseQuery()
        query.setObject(tokenData, forKey = kSecValueData as NSString)

        val status = SecItemAdd(query as CFDictionaryRef, null)
        if (status == 0) {
            hasTokenState.value = true
        } else {
            throw Exception("Failed to save token to Keychain: error code $status")
        }
    }

    actual override suspend fun getToken(): String? = retrieveTokenFromKeychain()

    actual override fun hasToken(): Flow<Boolean> = hasTokenState.asStateFlow()

    actual override suspend fun clearToken() {
        deleteTokenFromKeychain()
        hasTokenState.value = false
    }

    private fun retrieveTokenFromKeychain(): String? {
        val query = buildBaseQuery()
        query.setObject(true, forKey = kSecReturnData as NSString)
        query.setObject(kSecMatchLimitOne, forKey = kSecMatchLimit as NSString)

        return memScoped {
            val resultRef = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, resultRef.ptr)
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

    private fun deleteTokenFromKeychain() {
        val query = buildBaseQuery()
        val status = SecItemDelete(query as CFDictionaryRef)
        // errSecItemNotFound (-25300) is acceptable — nothing to delete
        if (status != 0 && status != -25300) {
            throw Exception("Failed to delete token from Keychain: error code $status")
        }
    }

    private fun buildBaseQuery(): NSMutableDictionary =
        NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = kSecClass as NSString)
            setObject(SERVICE_ID, forKey = kSecAttrService as NSString)
            setObject(ACCOUNT_ID, forKey = kSecAttrAccount as NSString)
            setObject(
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                forKey = kSecAttrAccessible as NSString,
            )
        }

    companion object {
        private const val SERVICE_ID = "com.weatherify.auth"
        private const val ACCOUNT_ID = "auth_token"
    }
}
