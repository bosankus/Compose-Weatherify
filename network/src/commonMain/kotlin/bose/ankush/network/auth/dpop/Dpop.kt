package bose.ankush.network.auth.dpop

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

const val DPOP_SCHEME = "DPoP"
private const val BEARER_SCHEME = "Bearer"
private const val DPOP_HEADER = "DPoP"
private const val DPOP_NONCE_HEADER = "DPoP-Nonce"

/**
 * The Authorization scheme for a stored token: "DPoP" once the server says it bound the token to
 * the device key, otherwise "Bearer". Proofs go out either way, so a server can start binding
 * tokens whenever it's ready.
 */
fun authorizationScheme(tokenType: String?): String =
    if (tokenType.equals(DPOP_SCHEME, ignoreCase = true)) DPOP_SCHEME else BEARER_SCHEME

private class DpopBinding(
    val accessToken: String?,
)

private val DpopBindingKey = AttributeKey<DpopBinding>("DpopBinding")

/**
 * Asks for a DPoP proof on this request. Only requests to this app's API are marked, never the
 * shared client's other traffic. [accessToken] is the token in its Authorization header, if any.
 */
fun HttpRequestBuilder.dpop(accessToken: String? = null) {
    attributes.put(DpopBindingKey, DpopBinding(accessToken))
}

/** Authorization for [token] under [scheme], plus the proof that goes with it. */
fun HttpRequestBuilder.authorize(
    token: String?,
    scheme: String,
) {
    val value = token?.takeIf { it.isNotBlank() } ?: return
    header(HttpHeaders.Authorization, "$scheme $value")
    dpop(accessToken = value)
}

class DpopConfig {
    var proofs: DpopProofFactory? = null

    /** A proof that couldn't be made; the request goes without one and the server decides. */
    var onProofFailed: (Throwable) -> Unit = {}
}

/**
 * Signs a fresh proof onto every marked request, and when the server answers with a new nonce
 * (RFC 9449 §8–9: 401 or 400 carrying `DPoP-Nonce`), retries once with it.
 */
val Dpop =
    createClientPlugin("DPoP", ::DpopConfig) {
        val proofs = checkNotNull(pluginConfig.proofs) { "DPoP plugin needs a proof factory" }
        val onProofFailed = pluginConfig.onProofFailed
        val nonce = MutableStateFlow<String?>(null)

        suspend fun HttpRequestBuilder.attachProof(
            binding: DpopBinding,
            withNonce: String?,
        ) {
            headers.remove(DPOP_HEADER)
            try {
                val htu = URLBuilder(url).apply {
                    encodedParameters.clear()
                    fragment = ""
                }.buildString()
                header(
                    DPOP_HEADER,
                    proofs.create(method.value, htu, binding.accessToken, withNonce)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                onProofFailed(e)
            }
        }

        on(Send) { request ->
            val binding = request.attributes.getOrNull(DpopBindingKey) ?: return@on proceed(request)
            val sentNonce = nonce.value
            request.attachProof(binding, sentNonce)
            val call = proceed(request)

            val issued = call.response.headers[DPOP_NONCE_HEADER] ?: return@on call
            nonce.value = issued
            val status = call.response.status
            val nonceDemanded =
                status == HttpStatusCode.Unauthorized || status == HttpStatusCode.BadRequest
            if (!nonceDemanded || issued == sentNonce) return@on call

            request.attachProof(binding, issued)
            proceed(request)
        }
    }
