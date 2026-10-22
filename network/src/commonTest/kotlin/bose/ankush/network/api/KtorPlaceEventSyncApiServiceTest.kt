package bose.ankush.network.api

import bose.ankush.network.auth.model.AuthResponse
import bose.ankush.network.auth.repository.AuthRepository
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.CreatePlaceEventBody
import bose.ankush.network.model.UpdatePlaceEventBody
import bose.ankush.tokenstorage.api.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Routes, bodies and status handling of [KtorPlaceEventSyncApiService] against MockEngine. */
class KtorPlaceEventSyncApiServiceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private var reply: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        respond("", HttpStatusCode.NoContent)
    }

    private val api =
        KtorPlaceEventSyncApiService(
            httpClient =
                HttpClient(
                    MockEngine { request ->
                        requests += request
                        reply(request)
                    },
                ) {
                    install(ContentNegotiation) { json(Json { encodeDefaults = true }) }
                },
            tokenManager = TokenManager(FakeTokenStorage(), NoAuthRepository()),
            baseUrl = "https://api.test",
        )

    @Test
    fun createPostsTheClientIdAndReadsTheDto() =
        runTest {
            reply = { json(HttpStatusCode.Created, envelope(DTO)) }

            val result = api.create(CREATE)

            val request = requests.single()
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/place-events", request.url.encodedPath)
            assertEquals("Bearer jwt", request.headers[HttpHeaders.Authorization])
            assertEquals(CREATE.id, bodyOf(request)["id"]?.jsonPrimitive?.content)
            assertEquals(201, result.statusCode)
            assertEquals("2026-10-22T10:00:00.000Z", result.data?.updatedAt)
            assertNull(result.data?.note)
        }

    @Test
    fun anExistingTombstoneComesBackOn200() =
        runTest {
            reply = { json(HttpStatusCode.OK, envelope(DTO.replace("\"deletedAt\":null", "\"deletedAt\":\"$T2\""))) }

            val result = api.create(CREATE)

            assertEquals(200, result.statusCode)
            assertEquals(T2, result.data?.deletedAt)
        }

    @Test
    fun updatePutsBaseUpdatedAtAndAConflictCarriesTheServerCopy() =
        runTest {
            reply = { json(HttpStatusCode.Conflict, envelope(DTO, status = false)) }

            val result = api.update(ID, UpdatePlaceEventBody("Harbor", 1.0, 2.0, "Lunch", T1, baseUpdatedAt = T1))

            val request = requests.single()
            assertEquals(HttpMethod.Put, request.method)
            assertEquals("/place-events/$ID", request.url.encodedPath)
            assertEquals(T1, bodyOf(request)["baseUpdatedAt"]?.jsonPrimitive?.content)
            assertEquals(409, result.statusCode)
            assertEquals("Dinner", result.data?.title)
        }

    @Test
    fun aConflictIsReadOnlyAsTheDtoDirectlyInData() =
        runTest {
            reply = { json(HttpStatusCode.Conflict, envelope("{\"current\":$DTO}", status = false)) }

            assertNull(api.update(ID, UpdatePlaceEventBody("H", 1.0, 2.0, "L", T1)).data)
        }

    @Test
    fun aBadRequestCarriesTheServerMessage() =
        runTest {
            reply = { json(HttpStatusCode.BadRequest, envelope("null", status = false, message = "title is required")) }

            val post = api.create(CREATE)
            val put = api.update(ID, UpdatePlaceEventBody("H", 1.0, 2.0, "", T1))

            assertEquals(400, post.statusCode)
            assertEquals("title is required", post.message)
            assertNull(post.data)
            assertEquals("title is required", put.message)
        }

    @Test
    fun aBodyThatIsNotAnEnvelopeHasNoMessage() =
        runTest {
            reply = { respond("<html>busy</html>", HttpStatusCode.ServiceUnavailable) }

            val result = api.changes(since = "c1")

            assertEquals(503, result.statusCode)
            assertNull(result.message)
            assertNull(result.data)
        }

    @Test
    fun deleteReportsTheStatusWithoutABody() =
        runTest {
            reply = { json(HttpStatusCode.NotFound, envelope("null", status = false)) }

            val result = api.delete(ID)

            assertEquals(HttpMethod.Delete, requests.single().method)
            assertEquals(404, result.statusCode)
            assertEquals("m", result.message)
        }

    @Test
    fun changesSendsSinceAndReadsThePage() =
        runTest {
            reply = {
                json(
                    HttpStatusCode.OK,
                    envelope("{\"items\":[$DTO],\"nextSince\":\"c1\",\"checkpoint\":\"c1\",\"hasMore\":true}"),
                )
            }

            val page = api.changes(since = "2026-10-22T10:00:00.000Z~0123456789abcdef01234567").data

            val request = requests.single()
            assertEquals("/place-events/sync", request.url.encodedPath)
            assertEquals("2026-10-22T10:00:00.000Z~0123456789abcdef01234567", request.url.parameters["since"])
            assertEquals(listOf(ID), page?.items?.map { it.id })
            assertEquals(true, page?.hasMore)
            assertEquals("c1", page?.nextSince)
        }

    @Test
    fun theFirstSyncSendsNoSince() =
        runTest {
            reply =
                {
                    json(
                        HttpStatusCode.OK,
                        envelope("{\"items\":[],\"nextSince\":null,\"checkpoint\":null,\"hasMore\":false}"),
                    )
                }

            api.changes(since = null)

            assertNull(requests.single().url.parameters["since"])
        }

    @Test
    fun aRateLimitReportsRetryAfterSeconds() =
        runTest {
            reply = { respond("Too many", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "17")) }

            val result = api.changes(since = null)

            assertEquals(429, result.statusCode)
            assertEquals(17L, result.retryAfterSeconds)
            assertNull(result.data)
        }

    private fun MockRequestHandleScope.json(
        status: HttpStatusCode,
        body: String,
    ): HttpResponseData = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun bodyOf(request: HttpRequestData) =
        Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject

    private fun envelope(
        data: String,
        status: Boolean = true,
        message: String = "m",
    ) = "{\"status\":$status,\"message\":\"$message\",\"data\":$data}"

    private companion object {
        const val ID = "0b5d8a8e-5a3c-4c4f-9a0e-2f6f7f1d2c3b"
        const val T1 = "2026-10-22T10:00:00.000Z"
        const val T2 = "2026-10-22T11:00:00.000Z"
        const val DTO =
            "{\"id\":\"$ID\",\"placeName\":\"Harbor\",\"lat\":1.0,\"lon\":2.0,\"title\":\"Dinner\"," +
                "\"startsAt\":\"2026-10-24T14:00:00Z\",\"createdAt\":\"$T1\",\"updatedAt\":\"$T1\",\"deletedAt\":null}"
        val CREATE = CreatePlaceEventBody(ID, "Harbor", 1.0, 2.0, "Dinner", "2026-10-24T14:00:00Z")
    }
}

private class FakeTokenStorage : TokenStorage {
    override suspend fun saveToken(token: String) = Unit

    override suspend fun getToken(): String = "jwt"

    override fun hasToken(): Flow<Boolean> = flowOf(true)

    override suspend fun clearToken() = Unit
}

private class NoAuthRepository : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
    ): AuthResponse = error("unused")

    override suspend fun register(
        email: String,
        password: String,
        timestampOfRegistration: String?,
        deviceModel: String?,
        operatingSystem: String?,
        osVersion: String?,
        appVersion: String?,
        registrationSource: String?,
        firebaseToken: String?,
    ): AuthResponse = error("unused")

    override fun isLoggedIn(): Flow<Boolean> = flowOf(true)

    override suspend fun getToken(): String = "jwt"

    override suspend fun refreshToken(): AuthResponse? = null

    override suspend fun logout(): Result<Unit> = Result.success(Unit)
}
