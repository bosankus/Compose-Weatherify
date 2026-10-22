package bose.ankush.network.api

import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.CreatePlaceEventBody
import bose.ankush.network.model.PlaceEventApiResult
import bose.ankush.network.model.PlaceEventDto
import bose.ankush.network.model.PlaceEventSyncPage
import bose.ankush.network.model.UpdatePlaceEventBody
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class KtorPlaceEventSyncApiService(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager,
    private val baseUrl: String,
) : PlaceEventSyncApiService {
    override suspend fun create(body: CreatePlaceEventBody): PlaceEventApiResult<PlaceEventDto> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                post("$baseUrl/place-events") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                    authConfig()
                }
            }.toResult(PlaceEventDto.serializer())

    override suspend fun update(
        id: String,
        body: UpdatePlaceEventBody,
    ): PlaceEventApiResult<PlaceEventDto> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                put("$baseUrl/place-events/${id.encodeURLPathPart()}") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                    authConfig()
                }
            }.toResult(PlaceEventDto.serializer())

    override suspend fun delete(id: String): PlaceEventApiResult<Unit> {
        val response =
            httpClient.authorizedRequest(tokenManager) { authConfig ->
                delete("$baseUrl/place-events/${id.encodeURLPathPart()}") { authConfig() }
            }
        return PlaceEventApiResult(
            statusCode = response.status.value,
            retryAfterSeconds = response.retryAfterSeconds(),
            message = response.envelope().message(),
        )
    }

    override suspend fun changes(since: String?): PlaceEventApiResult<PlaceEventSyncPage> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                get("$baseUrl/place-events/sync") {
                    since?.let { parameter("since", it) }
                    authConfig()
                }
            }.toResult(PlaceEventSyncPage.serializer())
}

private val envelopeJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

/**
 * Status, `Retry-After`, the envelope's `message` and its `data`, decoded with [serializer] when
 * present. A 409 carries the current server copy directly as `data`. A body that does not parse
 * (an empty 204, an HTML 429 page) leaves [PlaceEventApiResult.data] and the message null.
 */
private suspend fun <T> HttpResponse.toResult(serializer: KSerializer<T>): PlaceEventApiResult<T> {
    val envelope = envelope()
    val data =
        (envelope?.get("data") as? JsonObject)?.let { payload ->
            runCatching { envelopeJson.decodeFromJsonElement(serializer, payload) }.getOrNull()
        }
    return PlaceEventApiResult(status.value, data, retryAfterSeconds(), envelope.message())
}

/** The `{status, message, data}` envelope, or null when the body is not a JSON object. */
private suspend fun HttpResponse.envelope(): JsonObject? =
    runCatching { envelopeJson.parseToJsonElement(bodyAsText()).jsonObject }.getOrNull()

/** The envelope's `message`, e.g. the reason for a 400. */
private fun JsonObject?.message(): String? =
    (this?.get("message") as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.content
        ?.takeIf { it.isNotBlank() }

/** `Retry-After` in seconds. An HTTP-date value is ignored, so the caller backs off on its own. */
private fun HttpResponse.retryAfterSeconds(): Long? =
    headers[HttpHeaders.RetryAfter]
        ?.trim()
        ?.toLongOrNull()
        ?.takeIf { it >= 0 }
