package bose.ankush.network.api

import bose.ankush.network.auth.interceptor.authorizedRequest
import bose.ankush.network.auth.token.TokenManager
import bose.ankush.network.model.ApiResponse
import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.model.SaveLocationRequest
import bose.ankush.network.model.SavedLocation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorLocationApiService(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager,
    private val baseUrl: String
) : LocationApiService {
    override suspend fun saveLocation(request: SaveLocationRequest): ApiResponse<Unit> =
        httpClient.authorizedRequest(tokenManager) { authConfig ->
            post("$baseUrl/save-location") {
                contentType(ContentType.Application.Json)
                setBody(request)
                authConfig()
            }
        }.body()

    override suspend fun getSavedLocations(): ApiResponse<List<SavedLocation>> =
        httpClient.authorizedRequest(tokenManager) { authConfig ->
            get("$baseUrl/saved-places") { authConfig() }
        }.body()

    override suspend fun deleteLocation(id: String): ApiResponse<Unit> =
        httpClient.authorizedRequest(tokenManager) { authConfig ->
            delete("$baseUrl/saved-places/$id") { authConfig() }
        }.body()

    override suspend fun searchPlaces(query: String): ApiResponse<List<PlaceSuggestion>> =
        httpClient.authorizedRequest(tokenManager) { authConfig ->
            get("$baseUrl/search-place") {
                parameter("q", query)
                authConfig()
            }
        }.body()
}
