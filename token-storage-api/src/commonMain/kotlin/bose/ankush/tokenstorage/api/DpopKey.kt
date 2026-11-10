package bose.ankush.tokenstorage.api

/**
 * The device-bound key behind DPoP (RFC 9449): an EC P-256 key pair whose private half lives in
 * secure hardware (Android Keystore / iOS Secure Enclave) and can be used to sign but never read.
 * The server binds the session's tokens to its public key, so a copied token is useless without it.
 */
interface DpopKey {
    /** The public key's affine coordinates, 32 bytes each. Creates the key pair on first use. */
    suspend fun publicKey(): EcPublicKey

    /**
     * Signs [data] with ECDSA over SHA-256, returning the DER (X9.62) signature both platforms
     * produce natively. Creates the key pair on first use.
     */
    suspend fun sign(data: ByteArray): ByteArray

    /** Deletes the key pair; the next use creates a new one. */
    suspend fun delete()
}

class EcPublicKey(
    val x: ByteArray,
    val y: ByteArray,
)
