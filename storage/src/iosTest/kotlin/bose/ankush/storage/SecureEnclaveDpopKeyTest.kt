package bose.ankush.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyVerifySignature
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Real key generation and signing (the Secure Enclave where the Simulator has one). Kept in memory:
 * test binaries have no Keychain, so finding a stored key isn't covered here.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class SecureEnclaveDpopKeyTest {
    private val key = SecureEnclaveDpopKey(persistent = false)

    @AfterTest
    fun cleanUp() = runBlocking { key.delete() }

    @Test
    fun signaturesVerifyAgainstTheExportedPublicKey() =
        runBlocking {
            val message = "header.claims".encodeToByteArray()
            val publicKey = key.publicKey()

            assertEquals(32, publicKey.x.size)
            assertEquals(32, publicKey.y.size)
            val point = byteArrayOf(0x04) + publicKey.x + publicKey.y
            assertTrue(verifies(point, message, key.sign(message)))
            assertFalse(verifies(point, "tampered".encodeToByteArray(), key.sign(message)))
        }

    @Test
    fun theSameKeySignsUntilDeletedThenANewOneIsMade() =
        runBlocking {
            val first = key.publicKey().x
            assertTrue(first.contentEquals(key.publicKey().x))

            key.delete()

            assertFalse(first.contentEquals(key.publicKey().x))
        }

    private fun verifies(
        point: ByteArray,
        message: ByteArray,
        signature: ByteArray,
    ): Boolean {
        val attributes = CFDictionaryCreateMutable(
            null,
            2,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        )
        CFDictionaryAddValue(attributes, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
        CFDictionaryAddValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)
        val publicKey = SecKeyCreateWithData(point.cfData(), attributes, null) ?: return false
        return SecKeyVerifySignature(
            publicKey,
            kSecKeyAlgorithmECDSASignatureMessageX962SHA256,
            message.cfData(),
            signature.cfData(),
            null,
        )
    }

    private fun ByteArray.cfData() =
        CFBridgingRetain(usePinned {
            NSData.create(
                bytes = it.addressOf(0),
                length = size.convert()
            )
        })
            ?.reinterpret<cnames.structs.__CFData>()
}
