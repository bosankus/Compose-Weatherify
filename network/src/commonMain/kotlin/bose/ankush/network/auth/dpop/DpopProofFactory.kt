package bose.ankush.network.auth.dpop

import bose.ankush.tokenstorage.api.DpopKey
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Builds DPoP proofs (RFC 9449 §4.2): a JWT signed by the device key, good for one request. It
 * names the method and URL it was made for, when it was made, and, alongside an access token, a
 * hash of that token, so it can't be replayed, moved to another endpoint, or paired with another
 * token.
 */
class DpopProofFactory(
    private val key: DpopKey,
    private val clock: Clock = Clock.System,
) {
    /**
     * [htu] is the request URL without query or fragment. [accessToken] is the token sent in this
     * request's Authorization header, if any. [nonce] is the latest one the server issued.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun create(
        htm: String,
        htu: String,
        accessToken: String?,
        nonce: String?,
    ): String {
        val publicKey = key.publicKey()
        val header =
            buildJsonObject {
                put("typ", "dpop+jwt")
                put("alg", "ES256")
                putJsonObject("jwk") {
                    put("kty", "EC")
                    put("crv", "P-256")
                    put("x", publicKey.x.base64Url())
                    put("y", publicKey.y.base64Url())
                }
            }
        val claims =
            buildJsonObject {
                put("jti", Uuid.random().toString())
                put("htm", htm)
                put("htu", htu)
                put("iat", clock.now().epochSeconds)
                accessToken?.let { put("ath", sha256(it.encodeToByteArray()).base64Url()) }
                nonce?.let { put("nonce", it) }
            }
        val signingInput =
            header.toString().encodeToByteArray().base64Url() + "." +
                    claims.toString().encodeToByteArray().base64Url()
        val signature = derToJose(key.sign(signingInput.encodeToByteArray()))
        return signingInput + "." + signature.base64Url()
    }
}

@OptIn(ExperimentalEncodingApi::class)
private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.base64Url(): String = base64Url.encode(this)

private const val P256_PART_BYTES = 32
private const val DER_SEQUENCE: Byte = 0x30
private const val DER_INTEGER: Byte = 0x02
private const val DER_LONG_LENGTH = 0x80
private const val BYTE_MASK = 0xFF

/**
 * Keystore and the Security framework sign in DER (`SEQUENCE { INTEGER r, INTEGER s }`); JWS
 * ES256 wants r and s as two fixed 32-byte big-endian halves.
 */
internal fun derToJose(der: ByteArray): ByteArray {
    require(der.size > 2 && der[0] == DER_SEQUENCE) { "Not a DER ECDSA signature" }
    var offset = 2
    // P-256 signatures always fit the short length form, but read the long form rather than misparse.
    if (der[1].toInt() and DER_LONG_LENGTH != 0) offset += der[1].toInt() and (DER_LONG_LENGTH - 1)

    fun readInteger(): ByteArray {
        require(offset + 2 <= der.size && der[offset] == DER_INTEGER) { "Malformed DER ECDSA signature" }
        val length = der[offset + 1].toInt() and BYTE_MASK
        val start = offset + 2
        require(start + length <= der.size) { "Truncated DER ECDSA signature" }
        offset = start + length
        return der.copyOfRange(start, start + length)
    }

    return readInteger().toFixedLength() + readInteger().toFixedLength()
}

/** DER integers are minimal and signed: drop the sign byte, then left-pad to 32 bytes. */
private fun ByteArray.toFixedLength(): ByteArray {
    val magnitude = dropWhile { it == 0.toByte() }.toByteArray()
    require(magnitude.size <= P256_PART_BYTES) { "ECDSA value longer than P-256 allows" }
    return ByteArray(P256_PART_BYTES - magnitude.size) + magnitude
}
