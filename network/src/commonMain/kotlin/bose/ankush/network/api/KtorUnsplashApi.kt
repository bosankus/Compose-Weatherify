package bose.ankush.network.api

import bose.ankush.network.model.UnsplashPhoto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

class KtorUnsplashApi(
    private val httpClient: HttpClient,
    private val accessKey: String,
) : UnsplashApi {
    override suspend fun searchPhotos(
        query: String,
        perPage: Int,
    ): List<UnsplashPhoto> {
        requireKey()
        val response =
            httpClient
                .get("https://api.unsplash.com/search/photos") {
                    header(HttpHeaders.Authorization, "Client-ID $accessKey")
                    header("Accept-Version", "v1")
                    parameter("query", query)
                    parameter("per_page", perPage)
                    parameter("orientation", "portrait")
                    parameter("content_filter", "high")
                }.body<SearchResponse>()
        return response.results
    }

    override suspend fun trackDownload(downloadLocation: String) {
        requireKey()
        httpClient.get(downloadLocation) {
            header(HttpHeaders.Authorization, "Client-ID $accessKey")
            header("Accept-Version", "v1")
        }
    }

    private fun requireKey() {
        check(accessKey.isNotBlank()) { "Unsplash access key is not configured" }
    }

    @Serializable
    private data class SearchResponse(
        val results: List<UnsplashPhoto> = emptyList(),
    )
}
