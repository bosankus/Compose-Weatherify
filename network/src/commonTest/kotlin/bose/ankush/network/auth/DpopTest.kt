package bose.ankush.network.auth

import bose.ankush.network.FakeDpopKey
import bose.ankush.network.auth.api.KtorAuthApiService
import bose.ankush.network.auth.dpop.Dpop
import bose.ankush.network.auth.dpop.DpopProofFactory
import bose.ankush.network.auth.dpop.authorize
import bose.ankush.network.auth.dpop.derToJose
import bose.ankush.network.auth.dpop.sha256
import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.repository.AuthRepositoryImpl
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.tokenstorage.api.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DpopTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val key = FakeDpopKey()

    @Test
    fun derSignaturesBecomeFixedLengthHalves() {
        val r = ByteArray(32) { 0x7F }
        // A high first byte makes DER prepend a 0x00 sign byte, giving a 33-byte integer.
        val s = ByteArray(32) { 0x80.toByte() }
        val der = byteArrayOf(0x30, 69, 0x02, 32) + r + byteArrayOf(0x02, 33, 0x00) + s

        assertContentEquals(r + s, derToJose(der))
        assertContentEquals(
            ByteArray(31) + byteArrayOf(1) + ByteArray(31) + byteArrayOf(2),
            derToJose(byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x01, 0x02, 0x01, 0x02)),
        )
    }

    @Test
    fun sha256MatchesTheStandardVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256("abc".encodeToByteArray()).joinToString("") {
                (it.toInt() and 0xFF).toString(16).padStart(2, '0')
            },
        )
    }

    @Test
    fun aProofNamesItsKeyRequestTimeAndToken() =
        runTest {
            val proof =
                DpopProofFactory(key, FixedClock).create(
                    htm = "GET",
                    htu = "https://api.test/data",
                    accessToken = "abc",
                    nonce = "n1",
                )

            val (header, claims, signature) = proof.split(".")
            val headerJson = header.decodeJson()
            assertEquals("dpop+jwt", headerJson.string("typ"))
            assertEquals("ES256", headerJson.string("alg"))
            val jwk = headerJson.getValue("jwk").jsonObject
            assertEquals("EC", jwk.string("kty"))
            assertEquals("P-256", jwk.string("crv"))
            assertEquals(32, jwk.string("x").decodeBase64Url().size)

            val claimsJson = claims.decodeJson()
            assertEquals("GET", claimsJson.string("htm"))
            assertEquals("https://api.test/data", claimsJson.string("htu"))
            assertEquals(1_700_000_000, claimsJson.getValue("iat").jsonPrimitive.long)
            // base64url(SHA-256("abc")), the RFC's access token hash.
            assertEquals("ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0", claimsJson.string("ath"))
            assertEquals("n1", claimsJson.string("nonce"))
            assertTrue(claimsJson.string("jti").isNotBlank())

            assertEquals("$header.$claims", key.signed.single())
            assertEquals(64, signature.decodeBase64Url().size)
        }

    @Test
    fun authorizedRequestsCarryAProofForTheirUrlWithoutQueryOrFragment() =
        runTest {
            val client = clientRespondingWith { respondJson("{}") }

            client.get("https://api.test/data?city=Pune#top") { authorize("t1", "Bearer") }

            val request = requests.single()
            assertEquals("Bearer t1", request.headers[HttpHeaders.Authorization])
            val claims = request.proofClaims()
            assertEquals("https://api.test/data", claims.string("htu"))
            assertEquals("GET", claims.string("htm"))
        }

    @Test
    fun unmarkedRequestsGetNoProof() =
        runTest {
            val client = clientRespondingWith { respondJson("{}") }

            client.get("https://images.example.com/photo.jpg")

            assertNull(requests.single().headers["DPoP"])
            assertTrue(key.signed.isEmpty())
        }

    @Test
    fun aDemandedNonceIsRetriedOnceAndUsedFromThenOn() =
        runTest {
            val client =
                clientRespondingWith { request ->
                    val nonce = request.proofClaims()["nonce"]?.jsonPrimitive?.content
                    if (nonce == "n1") {
                        respondJson("{}")
                    } else {
                        respondJson("{}", HttpStatusCode.Unauthorized, nonce = "n1")
                    }
                }

            val status = client.get("https://api.test/data") { authorize("t1", "DPoP") }.status
            client.get("https://api.test/other") { authorize("t1", "DPoP") }

            assertEquals(HttpStatusCode.OK, status)
            assertEquals(
                listOf(null, "n1", "n1"),
                requests.map { it.proofClaims()["nonce"]?.jsonPrimitive?.content })
        }

    @Test
    fun aRefusalWithANonceAlreadyUsedIsNotRetried() =
        runTest {
            val client = clientRespondingWith {
                respondJson(
                    "{}",
                    HttpStatusCode.Unauthorized,
                    nonce = "n1"
                )
            }

            client.get("https://api.test/data") { authorize("t1", "DPoP") }
            requests.clear()
            client.get("https://api.test/data") { authorize("t1", "DPoP") }

            assertEquals(1, requests.size)
        }

    @Test
    fun signingInRotatesTheKeyAndADpopTokenSwitchesTheScheme() =
        runTest {
            val storage = MemoryTokenStorage()
            val client =
                clientRespondingWith { request ->
                    when (request.url.encodedPath) {
                        "/login" -> respondJson("""{"status":true,"data":{"token":"bound","tokenType":"DPoP"}}""")
                        else -> respondJson("{}")
                    }
                }
            val repository = AuthRepositoryImpl(KtorAuthApiService(client, BASE_URL), storage, key)
            val tokenManager = TokenManager(storage, repository)

            repository.login("a@b.c", "pw")
            client.authorizedRequest(tokenManager) { auth -> get("$BASE_URL/data") { auth() } }

            assertEquals(1, key.deletions)
            val (login, data) = requests
            // The sign-in itself carries a proof, without a token hash: there is no token yet.
            assertFalse(login.proofClaims().containsKey("ath"))
            assertEquals("DPoP bound", data.headers[HttpHeaders.Authorization])
            assertTrue(data.proofClaims().containsKey("ath"))
        }

    private fun clientRespondingWith(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                requests += request
                handler(request)
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Dpop) { proofs = DpopProofFactory(key, FixedClock) }
        }

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        nonce: String? = null,
    ): HttpResponseData {
        val contentType = HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())
        val headers =
            if (nonce == null) headersOf(contentType) else headersOf(
                contentType,
                "DPoP-Nonce" to listOf(nonce)
            )
        return respond(body, status, headers)
    }

    private fun HttpRequestData.proofClaims(): JsonObject =
        headers["DPoP"]!!.split(".")[1].decodeJson()

    private fun String.decodeJson(): JsonObject =
        Json.parseToJsonElement(decodeBase64Url().decodeToString()).jsonObject

    @OptIn(ExperimentalEncodingApi::class)
    private fun String.decodeBase64Url(): ByteArray =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(this)

    private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content

    private object FixedClock : Clock {
        override fun now(): Instant = Instant.fromEpochSeconds(1_700_000_000)
    }

    private class MemoryTokenStorage : TokenStorage {
        private val access = MutableStateFlow<String?>(null)
        private var refresh: String? = null
        private var type: String? = null

        override suspend fun saveTokens(
            accessToken: String,
            refreshToken: String?,
            tokenType: String?,
        ) {
            access.value = accessToken
            refresh = refreshToken
            type = tokenType
        }

        override suspend fun getToken(): String? = access.value

        override suspend fun getRefreshToken(): String? = refresh

        override suspend fun getTokenType(): String? = type

        override fun hasToken(): Flow<Boolean> = access.map { it != null }

        override suspend fun clearToken() {
            access.value = null
            refresh = null
            type = null
        }
    }

    private companion object {
        const val BASE_URL = "https://api.test"
    }
}
