package bose.ankush.network.auth.dpop

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
internal actual fun sha256(data: ByteArray): ByteArray {
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    data.usePinned { input ->
        digest.usePinned { output ->
            CC_SHA256(
                if (data.isEmpty()) null else input.addressOf(0),
                data.size.convert(),
                output.addressOf(0)
            )
        }
    }
    return digest.toByteArray()
}
