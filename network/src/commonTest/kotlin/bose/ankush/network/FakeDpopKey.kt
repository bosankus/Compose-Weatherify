package bose.ankush.network

import bose.ankush.tokenstorage.api.DpopKey
import bose.ankush.tokenstorage.api.EcPublicKey

/** A stand-in device key: fixed public key, and a fixed DER signature (r = 1, s = 2). */
class FakeDpopKey : DpopKey {
    var deletions = 0
        private set
    val signed = mutableListOf<String>()

    override suspend fun publicKey() = EcPublicKey(x = ByteArray(32) { 1 }, y = ByteArray(32) { 2 })

    override suspend fun sign(data: ByteArray): ByteArray {
        signed += data.decodeToString()
        return byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x01, 0x02, 0x01, 0x02)
    }

    override suspend fun delete() {
        deletions++
    }
}
