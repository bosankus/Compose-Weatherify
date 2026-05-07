package bose.ankush.network.api

import bose.ankush.network.model.FeedbackRequest
import bose.ankush.network.model.FeedbackResponse
import bose.ankush.network.utils.NetworkUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Ktor implementation of FeedbackApiService
 */
class KtorFeedbackApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : FeedbackApiService {
    override suspend fun submitFeedback(request: FeedbackRequest): FeedbackResponse =
        NetworkUtils.retryWithExponentialBackoff {
            httpClient
                .post("$baseUrl/feedback") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }
}
