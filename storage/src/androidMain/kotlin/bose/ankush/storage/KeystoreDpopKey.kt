package bose.ankush.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import bose.ankush.tokenstorage.api.DpopKey
import bose.ankush.tokenstorage.api.EcPublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

/**
 * The DPoP key in the Android Keystore: generated inside the TEE and marked for signing only, so
 * the private key can't be exported, even by this app.
 *
 * Not StrongBox: its secure element takes tens of milliseconds per signature, and this key signs
 * every API request. The TEE already keeps the key off the app's memory.
 */
class KeystoreDpopKey : DpopKey {
    private val keyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }

    // Two first requests at once must not each generate (and overwrite) a key.
    private val createLock = Mutex()

    override suspend fun publicKey(): EcPublicKey =
        withContext(Dispatchers.IO) {
            val key = entry().certificate.publicKey as ECPublicKey
            EcPublicKey(x = key.w.affineX.toCoordinate(), y = key.w.affineY.toCoordinate())
        }

    override suspend fun sign(data: ByteArray): ByteArray =
        withContext(Dispatchers.IO) {
            Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initSign(entry().privateKey as PrivateKey)
                update(data)
                sign()
            }
        }

    override suspend fun delete() {
        withContext(Dispatchers.IO) {
            createLock.withLock { if (keyStore.containsAlias(ALIAS)) keyStore.deleteEntry(ALIAS) }
        }
    }

    private suspend fun entry(): KeyStore.PrivateKeyEntry =
        createLock.withLock {
            existingEntry() ?: run {
                generate()
                checkNotNull(existingEntry()) { "DPoP key missing right after generation" }
            }
        }

    private fun existingEntry(): KeyStore.PrivateKeyEntry? =
        keyStore.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry

    private fun generate() {
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE).run {
            initialize(
                KeyGenParameterSpec
                    .Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
            generateKeyPair()
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "weatherify_dpop"
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        const val COORDINATE_BYTES = 32
    }

    /** A P-256 coordinate as exactly 32 unsigned big-endian bytes, as JWK expects. */
    private fun BigInteger.toCoordinate(): ByteArray {
        // toByteArray() is signed: it may carry an extra leading zero, or be short for a small value.
        val signed = toByteArray()
        val bytes =
            signed.copyOfRange((signed.size - COORDINATE_BYTES).coerceAtLeast(0), signed.size)
        return ByteArray(COORDINATE_BYTES - bytes.size) + bytes
    }
}
