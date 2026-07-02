package bose.ankush.network.repository

import bose.ankush.network.api.FeedbackApiService
import bose.ankush.network.model.FeedbackRequest
import bose.ankush.network.model.FeedbackResponse
import bose.ankush.network.util.NetworkConnectivity

class FeedbackRepositoryImpl(
    private val apiService: FeedbackApiService,
    private val networkConnectivity: NetworkConnectivity,
) : FeedbackRepository {
    override suspend fun submitFeedback(request: FeedbackRequest): Result<FeedbackResponse> {
        if (!networkConnectivity.isNetworkAvailable()) {
            return Result.failure(IllegalStateException("No internet connection"))
        }
        return runCatching { apiService.submitFeedback(request) }
    }
}
