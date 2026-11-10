package bose.ankush.network.auth

import bose.ankush.network.FakeDpopKey
import bose.ankush.network.auth.api.KtorAuthApiService
import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.repository.AuthRepositoryImpl
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.tokenstorage.api.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenSessionTest {
    private val requests = mutableListOf<Pair<String, String>>()

    @Test
    fun refreshSendsTheRefreshTokenAndKeepsTheRotatedPair() =
        runTest {
            val storage = MemoryTokenStorage("a1", "r1")
            val (_, tokenManager) =
                sessionRespondingWith { respondJson("""{"status":true,"data":{"token":"a2","refreshToken":"r2"}}""") }

            tokenManager(storage).refreshSession()

            val body = requests.single { it.first == "/refresh-token" }.second
            assertTrue(body.contains("\"refreshToken\":\"r1\""), body)
            assertEquals("a2", storage.getToken())
            assertEquals("r2", storage.getRefreshToken())
        }

    @Test
    fun aServerWithoutRefreshTokensNeverSeesTheField() =
        runTest {
            val storage = MemoryTokenStorage("a1", null)
            val (_, tokenManager) = sessionRespondingWith { respondJson("""{"status":true,"data":{"token":"a2"}}""") }

            tokenManager(storage).refreshSession()

            val body = requests.single { it.first == "/refresh-token" }.second
            assertFalse(body.contains("refreshToken"), body)
            assertEquals("a2", storage.getToken())
            assertNull(storage.getRefreshToken())
        }

    @Test
    fun requestsRefusedTogetherRefreshOnceAndShareTheNewToken() =
        runTest {
            val storage = MemoryTokenStorage("a1", "r1")
            val bothRefused = CompletableDeferred<Unit>()
            var refusals = 0
            val (client, tokenManager) =
                sessionRespondingWith { request ->
                    when {
                        request.url.encodedPath == "/refresh-token" ->
                            respondJson("""{"status":true,"data":{"token":"a2","refreshToken":"r2"}}""")

                        request.headers[HttpHeaders.Authorization] == "Bearer a1" -> {
                            // Hold the first refusal until the second arrives, so both carry a1.
                            if (++refusals == 2) bothRefused.complete(Unit)
                            bothRefused.await()
                            respondJson("""{"status":false}""", HttpStatusCode.Unauthorized)
                        }

                        else -> respondJson("""{"status":true}""")
                    }
                }
            val manager = tokenManager(storage)

            val statuses =
                List(2) {
                    async {
                        client.authorizedRequest(manager) { auth -> get("$BASE_URL/data") { auth() } }.status
                    }
                }.awaitAll()

            assertEquals(listOf(HttpStatusCode.OK, HttpStatusCode.OK), statuses)
            assertEquals(1, requests.count { it.first == "/refresh-token" })
        }

    @Test
    fun logoutSignsOutOnThisDeviceEvenWhenTheServerIsUnreachable() =
        runTest {
            val storage = MemoryTokenStorage("a1", "r1")
            val (client, _) = sessionRespondingWith { throw IllegalStateException("offline") }

            val result = AuthRepositoryImpl(
                KtorAuthApiService(client, BASE_URL),
                storage,
                FakeDpopKey()
            ).logout()

            assertTrue(result.isFailure)
            assertNull(storage.getToken())
            assertNull(storage.getRefreshToken())
        }

    private fun sessionRespondingWith(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Pair<HttpClient, (TokenStorage) -> TokenManager> {
        val client =
            HttpClient(
                MockEngine { request ->
                    requests += request.url.encodedPath to request.body.toByteArray()
                        .decodeToString()
                    handler(request)
                },
            ) {
                // As in NetworkModule: encodeDefaults is on there, which the request must not trip.
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            encodeDefaults = true
                        },
                    )
                }
            }
        return client to { storage ->
            TokenManager(
                storage,
                AuthRepositoryImpl(KtorAuthApiService(client, BASE_URL), storage, FakeDpopKey())
            )
        }
    }

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData =
        respond(
            body,
            status,
            headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )

    private class MemoryTokenStorage(
        access: String?,
        refresh: String?,
    ) : TokenStorage {
        private val access = MutableStateFlow(access)
        private var refresh: String? = refresh

        override suspend fun saveTokens(
            accessToken: String,
            refreshToken: String?,
            tokenType: String?,
        ) {
            access.value = accessToken
            refresh = refreshToken
        }

        override suspend fun getToken(): String? = access.value

        override suspend fun getRefreshToken(): String? = refresh

        override suspend fun getTokenType(): String? = null

        override fun hasToken(): Flow<Boolean> = access.map { it != null }

        override suspend fun clearToken() {
            access.value = null
            refresh = null
        }
    }

    private companion object {
        const val BASE_URL = "https://api.test"
    }
}
