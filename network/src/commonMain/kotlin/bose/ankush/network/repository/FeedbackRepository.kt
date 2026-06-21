package bose.ankush.network.repository

import bose.ankush.network.model.FeedbackRequest
import bose.ankush.network.model.FeedbackResponse

interface FeedbackRepository {
    suspend fun submitFeedback(request: FeedbackRequest): Result<FeedbackResponse>
}
