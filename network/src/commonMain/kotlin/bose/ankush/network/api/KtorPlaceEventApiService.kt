package bose.ankush.network.api

import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.ApiResponse
import bose.ankush.network.model.CreatePlaceEventRequest
import bose.ankush.network.model.PlaceEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorPlaceEventApiService(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager,
    private val baseUrl: String,
) : PlaceEventApiService {
    override suspend fun getPlaceEvents(
        lat: Double,
        lon: Double,
    ): ApiResponse<List<PlaceEvent>> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                get("$baseUrl/place-events") {
                    parameter("lat", lat)
                    parameter("lon", lon)
                    authConfig()
                }
            }.body()

    override suspend fun createPlaceEvent(request: CreatePlaceEventRequest): ApiResponse<PlaceEvent?> =
        httpClient
            .authorizedRequest(tokenManager) { authConfig ->
                post("$baseUrl/place-events") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    authConfig()
                }
            }.body()
}
