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
        return PlaceEventApiResult(response.status.value, retryAfterSeconds = response.retryAfterSeconds())
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
 * Status, `Retry-After` and the envelope's `data`, decoded with [serializer] when present. A 409
 * carries the server copy as `data`; a `data.current` wrapper is accepted too. A body that does
 * not parse (an empty 204, an HTML 429 page) leaves [PlaceEventApiResult.data] null.
 */
private suspend fun <T> HttpResponse.toResult(serializer: KSerializer<T>): PlaceEventApiResult<T> {
    val data =
        runCatching {
            val envelope = envelopeJson.parseToJsonElement(bodyAsText()).jsonObject
            val payload = envelope["data"] as? JsonObject
            val current = payload?.get("current") as? JsonObject
            (current ?: payload)?.let { envelopeJson.decodeFromJsonElement(serializer, it) }
        }.getOrNull()
    return PlaceEventApiResult(status.value, data, retryAfterSeconds())
}

/** `Retry-After` in seconds. An HTTP-date value is ignored, so the caller backs off on its own. */
private fun HttpResponse.retryAfterSeconds(): Long? =
    headers[HttpHeaders.RetryAfter]
        ?.trim()
        ?.toLongOrNull()
        ?.takeIf { it >= 0 }
