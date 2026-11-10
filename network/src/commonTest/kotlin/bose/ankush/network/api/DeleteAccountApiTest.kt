package bose.ankush.network.api

import bose.ankush.network.FakeDpopKey
import bose.ankush.network.auth.api.KtorAuthApiService
import bose.ankush.network.auth.events.AuthEvent
import bose.ankush.network.auth.events.AuthEventBus
import bose.ankush.network.auth.repository.AuthRepositoryImpl
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.DeleteAccountResult
import bose.ankush.tokenstorage.api.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteAccountApiTest {
    private val storage = MemoryTokenStorage("old-token")
    private val requests = mutableListOf<RecordedRequest>()

    @Test
    fun sendsThePasswordAsAJsonBodyOnADeleteWithTheBearerToken() =
        runTest {
            val api = apiRespondingWith { respondJson(HttpStatusCode.OK, DELETED) }

            val result = api.deleteAccount("hunter22")

            assertEquals(DeleteAccountResult.Deleted, result)
            val request = requests.single()
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("/account", request.path)
            assertEquals("Bearer old-token", request.authorization)
            assertTrue(request.contentType?.match(ContentType.Application.Json) == true)
            assertEquals("""{"password":"hunter22"}""", request.body)
        }

    @Test
    fun aWrongPasswordStaysSignedInAndNeverRefreshes() =
        runTest {
            val api =
                apiRespondingWith {
                    respondJson(
                        HttpStatusCode.Forbidden,
                        """{"status":false,"message":"Incorrect password","data":{"errorCode":"INVALID_PASSWORD"}}""",
                    )
                }

            val result = api.deleteAccount("wrong")

            assertEquals(DeleteAccountResult.IncorrectPassword, result)
            assertEquals("old-token", storage.getToken())
            assertEquals(listOf("/account"), requests.map { it.path })
        }

    @Test
    fun aForbiddenWithoutTheErrorCodeIsAFailureNotAWrongPassword() =
        runTest {
            val api =
                apiRespondingWith {
                    respondJson(
                        HttpStatusCode.Forbidden,
                        """{"status":false,"message":"Incorrect password"}"""
                    )
                }

            assertIs<DeleteAccountResult.Failed>(api.deleteAccount("pw"))
            assertEquals("old-token", storage.getToken())
        }

    @Test
    fun rateLimitingIsRecognisedEvenWithoutAnEnvelope() =
        runTest {
            val api =
                apiRespondingWith {
                    respond("Too Many Requests", HttpStatusCode.TooManyRequests, headersOf())
                }

            assertEquals(DeleteAccountResult.RateLimited, api.deleteAccount("pw"))
            assertEquals("old-token", storage.getToken())
        }

    @Test
    fun aServerErrorKeepsTheSession() =
        runTest {
            val api =
                apiRespondingWith {
                    respondJson(
                        HttpStatusCode.InternalServerError,
                        """{"status":false,"message":"boom"}"""
                    )
                }

            assertIs<DeleteAccountResult.Failed>(api.deleteAccount("pw"))
            assertEquals("old-token", storage.getToken())
        }

    @Test
    fun aDeletedAccountEndsTheSessionAfterOneRefreshAttempt() =
        runTest {
            val events = mutableListOf<AuthEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                AuthEventBus.events.collect { events += it }
            }
            val api =
                apiRespondingWith {
                    respondJson(
                        HttpStatusCode.Unauthorized,
                        """{"status":false,"message":"Unauthorized"}"""
                    )
                }

            val result = api.deleteAccount("pw")

            assertEquals(DeleteAccountResult.SessionExpired, result)
            assertNull(storage.getToken())
            assertEquals(listOf("/account", "/refresh-token"), requests.map { it.path })
            assertTrue(events.any { it is AuthEvent.Unauthorized })
        }

    @Test
    fun aRefreshedTokenThatIsStillRefusedEndsTheSessionInsteadOfLooping() =
        runTest {
            val api =
                apiRespondingWith { request ->
                    when (request.url.encodedPath) {
                        "/refresh-token" ->
                            respondJson(
                                HttpStatusCode.OK,
                                """{"status":true,"message":"ok","data":{"token":"new-token"}}""",
                            )

                        else ->
                            respondJson(
                                HttpStatusCode.Unauthorized,
                                """{"status":false,"message":"Unauthorized"}"""
                            )
                    }
                }

            val result = api.deleteAccount("pw")

            assertEquals(DeleteAccountResult.SessionExpired, result)
            assertNull(storage.getToken())
            assertEquals(listOf("/account", "/refresh-token", "/account"), requests.map { it.path })
            assertEquals("Bearer new-token", requests.last().authorization)
        }

    @Test
    fun anExpiredRefreshWindowEndsTheSession() =
        runTest {
            val api =
                apiRespondingWith { request ->
                    when (request.url.encodedPath) {
                        "/refresh-token" ->
                            respondJson(
                                HttpStatusCode.BadRequest,
                                """{"status":false,"message":"Token invalid","data":{"errorCode":"TOKEN_INVALID"}}""",
                            )

                        else ->
                            respondJson(
                                HttpStatusCode.Unauthorized,
                                """{"status":false,"message":"Unauthorized"}"""
                            )
                    }
                }

            assertEquals(DeleteAccountResult.SessionExpired, api.deleteAccount("pw"))
            assertNull(storage.getToken())
            assertEquals(listOf("/account", "/refresh-token"), requests.map { it.path })
        }

    @Test
    fun mapsEachStatusToItsOutcome() {
        assertEquals(DeleteAccountResult.Deleted, deleteAccountResult(HttpStatusCode.OK, null))
        assertEquals(
            DeleteAccountResult.IncorrectPassword,
            deleteAccountResult(HttpStatusCode.Forbidden, INVALID_PASSWORD),
        )
        assertIs<DeleteAccountResult.Failed>(deleteAccountResult(HttpStatusCode.Forbidden, "OTHER"))
        assertEquals(
            DeleteAccountResult.SessionExpired,
            deleteAccountResult(HttpStatusCode.Unauthorized, null)
        )
        assertEquals(
            DeleteAccountResult.RateLimited,
            deleteAccountResult(HttpStatusCode.TooManyRequests, null)
        )
        assertEquals(
            DeleteAccountResult.InvalidRequest,
            deleteAccountResult(HttpStatusCode.BadRequest, null)
        )
        assertIs<DeleteAccountResult.Failed>(
            deleteAccountResult(
                HttpStatusCode.ServiceUnavailable,
                null
            )
        )
    }

    private fun apiRespondingWith(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): KtorAccountApiService {
        val client =
            HttpClient(
                MockEngine { request ->
                    requests +=
                        RecordedRequest(
                            method = request.method,
                            path = request.url.encodedPath,
                            authorization = request.headers[HttpHeaders.Authorization],
                            contentType = request.body.contentType,
                            body = request.body.toByteArray().decodeToString(),
                        )
                    handler(request)
                },
            ) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        val authRepository =
            AuthRepositoryImpl(KtorAuthApiService(client, BASE_URL), storage, FakeDpopKey())
        return KtorAccountApiService(client, TokenManager(storage, authRepository), BASE_URL)
    }

    private fun MockRequestHandleScope.respondJson(
        status: HttpStatusCode,
        body: String,
    ): HttpResponseData =
        respond(
            body,
            status,
            headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )

    private class RecordedRequest(
        val method: HttpMethod,
        val path: String,
        val authorization: String?,
        val contentType: ContentType?,
        val body: String,
    )

    private class MemoryTokenStorage(
        initial: String?,
    ) : TokenStorage {
        private val token = MutableStateFlow(initial)

        override suspend fun saveTokens(
            accessToken: String,
            refreshToken: String?,
            tokenType: String?,
        ) {
            token.value = accessToken
        }

        override suspend fun getToken(): String? = token.value

        override suspend fun getRefreshToken(): String? = null

        override suspend fun getTokenType(): String? = null

        override fun hasToken(): Flow<Boolean> = token.map { it != null }

        override suspend fun clearToken() {
            token.value = null
        }
    }

    private companion object {
        const val BASE_URL = "https://api.test"
        const val DELETED =
            """{"status":true,"message":"Account deleted successfully","data":null}"""
    }
}
