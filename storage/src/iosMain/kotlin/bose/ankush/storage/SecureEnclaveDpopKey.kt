package bose.ankush.storage

import bose.ankush.tokenstorage.api.DpopKey
import bose.ankush.tokenstorage.api.EcPublicKey
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyCreateSignature
import platform.Security.SecKeyRef
import platform.Security.kSecAccessControlPrivateKeyUsage
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrIsPermanent
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrTokenID
import platform.Security.kSecAttrTokenIDSecureEnclave
import platform.Security.kSecClass
import platform.Security.kSecClassKey
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import platform.Security.kSecPrivateKeyAttrs
import platform.Security.kSecReturnRef
import platform.posix.memcpy

/**
 * The DPoP key in the Secure Enclave: generated inside it and usable only for signing, so the
 * private key never exists outside the chip. Where there is no Secure Enclave it falls back to an
 * ordinary Keychain key, still this-device-only and never synced.
 *
 * Like the tokens, it is usable only while the device is unlocked.
 *
 * [persistent] is false only in tests: unit test binaries have no Keychain, so the key then lives
 * in memory for this instance's lifetime.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class SecureEnclaveDpopKey internal constructor(
    private val persistent: Boolean,
) : DpopKey {
    constructor() : this(persistent = true)

    private val lock = Mutex()
    private var cachedKey: SecKeyRef? = null

    override suspend fun publicKey(): EcPublicKey =
        lock.withLock {
            val publicKey = SecKeyCopyPublicKey(privateKey()) ?: error("DPoP key has no public key")
            try {
                // ANSI X9.63: 0x04 || X || Y, 32 bytes each for P-256.
                val point = copyData { error -> SecKeyCopyExternalRepresentation(publicKey, error) }
                EcPublicKey(
                    x = point.copyOfRange(1, 1 + COORDINATE_BYTES),
                    y = point.copyOfRange(1 + COORDINATE_BYTES, point.size),
                )
            } finally {
                CFRelease(publicKey)
            }
        }

    override suspend fun sign(data: ByteArray): ByteArray =
        lock.withLock {
            val key = privateKey()
            val input = CFBridgingRetain(data.toNSData())?.reinterpret<cnames.structs.__CFData>()
            try {
                copyData { error ->
                    SecKeyCreateSignature(
                        key,
                        kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
                        input,
                        error
                    )
                }
            } finally {
                CFRelease(input)
            }
        }

    override suspend fun delete() {
        lock.withLock {
            cachedKey?.let(::CFRelease)
            cachedKey = null
            val tag = tagData()
            val query = cfDictionary(kSecClass to kSecClassKey, kSecAttrApplicationTag to tag)
            SecItemDelete(query)
            CFRelease(query)
            CFRelease(tag)
        }
    }

    /** Callers hold [lock]. */
    private fun privateKey(): SecKeyRef {
        cachedKey?.let { return it }
        val failures = mutableListOf<String>()
        val key =
            (if (persistent) findKey() else null)
                ?: createKey(secureEnclave = true, failures)
                ?: createKey(secureEnclave = false, failures)
                ?: error("Couldn't create the DPoP key: ${failures.joinToString("; ")}")
        cachedKey = key
        return key
    }

    private fun findKey(): SecKeyRef? {
        val tag = tagData()
        val query =
            cfDictionary(
                kSecClass to kSecClassKey,
                kSecAttrApplicationTag to tag,
                kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                kSecReturnRef to kCFBooleanTrue,
            )
        return try {
            memScoped {
                val result = alloc<CFTypeRefVar>()
                if (SecItemCopyMatching(
                        query,
                        result.ptr
                    ) == 0
                ) result.value?.reinterpret() else null
            }
        } finally {
            CFRelease(query)
            CFRelease(tag)
        }
    }

    private fun createKey(
        secureEnclave: Boolean,
        failures: MutableList<String>,
    ): SecKeyRef? {
        val access =
            SecAccessControlCreateWithFlags(
                null,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                if (secureEnclave) kSecAccessControlPrivateKeyUsage else 0uL,
                null,
            ) ?: return null.also { failures += "access control unavailable" }
        val tag = tagData()
        val keySize = CFBridgingRetain(NSNumber(int = KEY_SIZE_BITS))
        val privateAttributes =
            cfDictionary(
                kSecAttrIsPermanent to if (persistent) kCFBooleanTrue else kCFBooleanFalse,
                kSecAttrApplicationTag to tag,
                kSecAttrAccessControl to access,
            )
        val attributes =
            cfDictionary(
                kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                kSecAttrKeySizeInBits to keySize,
                kSecPrivateKeyAttrs to privateAttributes,
            )
        if (secureEnclave) CFDictionaryAddValue(
            attributes,
            kSecAttrTokenID,
            kSecAttrTokenIDSecureEnclave
        )
        return try {
            memScoped {
                val error = alloc<CFErrorRefVar>()
                SecKeyCreateRandomKey(attributes, error.ptr).also {
                    if (it == null) {
                        val reason =
                            (CFBridgingRelease(error.value) as? NSError)?.localizedDescription
                        failures += "${if (secureEnclave) "Secure Enclave" else "Keychain"}: $reason"
                    }
                }
            }
        } finally {
            listOf(attributes, privateAttributes, keySize, tag, access).forEach(::CFRelease)
        }
    }

    private fun tagData(): CFTypeRef? =
        CFBridgingRetain((KEY_TAG as NSString).dataUsingEncoding(NSUTF8StringEncoding))

    private fun cfDictionary(vararg entries: Pair<CFTypeRef?, CFTypeRef?>): CFMutableDictionaryRef {
        val dictionary =
            CFDictionaryCreateMutable(
                null,
                entries.size.convert(),
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            ) ?: error("Couldn't create a CFDictionary")
        entries.forEach { (key, value) -> CFDictionaryAddValue(dictionary, key, value) }
        return dictionary
    }

    /** Runs a Security call that returns a new CFData (or fills its error) and copies the bytes out. */
    private fun copyData(call: (kotlinx.cinterop.CPointer<CFErrorRefVar>) -> CFDataRef?): ByteArray =
        memScoped {
            val error = alloc<CFErrorRefVar>()
            val data = call(error.ptr)
            if (data == null) {
                val reason = (CFBridgingRelease(error.value) as? NSError)?.localizedDescription
                error("DPoP key operation failed: ${reason ?: "unknown error"}")
            }
            (CFBridgingRelease(data) as NSData).toByteArray()
        }

    private fun ByteArray.toNSData(): NSData =
        usePinned { NSData.create(bytes = it.addressOf(0), length = size.convert()) }

    private fun NSData.toByteArray(): ByteArray =
        ByteArray(length.toInt()).also { bytes ->
            if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
        }

    private companion object {
        const val KEY_TAG = "com.weatherify.dpop"
        const val KEY_SIZE_BITS = 256
        const val COORDINATE_BYTES = 32
    }
}
