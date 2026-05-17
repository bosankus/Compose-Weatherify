package bose.ankush.network.api

import bose.ankush.network.model.FeedbackRequest
import bose.ankush.network.model.FeedbackResponse

interface FeedbackApiService {
    suspend fun submitFeedback(request: FeedbackRequest): FeedbackResponse
}
