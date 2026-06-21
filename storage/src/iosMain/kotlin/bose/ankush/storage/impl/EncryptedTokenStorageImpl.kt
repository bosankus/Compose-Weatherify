@file:Suppress("UNCHECKED_CAST")

package bose.ankush.storage.impl

import bose.ankush.storage.api.TokenStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
@OptIn(ExperimentalForeignApi::class)
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
        query.setObject(tokenData, forKey = kSecValueData as Any)

        val status = SecItemAdd(query, null)
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
        query.setObject(true, forKey = kSecReturnData as Any)
        query.setObject(kSecMatchLimitOne, forKey = kSecMatchLimit as Any)

        return memScoped {
            val resultRef = alloc<ObjCObjectVar<Any?>>()
            val status = SecItemCopyMatching(query, resultRef.ptr)
            if (status == 0) {
                val nsData = resultRef.value as? NSData
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
        val status = SecItemDelete(query)
        // errSecItemNotFound (-25300) is acceptable — nothing to delete
        if (status != 0 && status != -25300) {
            throw Exception("Failed to delete token from Keychain: error code $status")
        }
    }

    private fun buildBaseQuery(): NSMutableDictionary =
        NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = kSecClass as Any)
            setObject(SERVICE_ID, forKey = kSecAttrService as Any)
            setObject(ACCOUNT_ID, forKey = kSecAttrAccount as Any)
            setObject(
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                forKey = kSecAttrAccessible as Any
            )
        }

    companion object {
        private const val SERVICE_ID = "com.weatherify.auth"
        private const val ACCOUNT_ID = "auth_token"
    }
}
