package bose.ankush.network.api

import bose.ankush.network.model.FeedbackRequest
import bose.ankush.network.model.FeedbackResponse

/**
 * API service interface for feedback operations
 */
interface FeedbackApiService {
    /**
     * Submit user feedback
     */
    suspend fun submitFeedback(request: FeedbackRequest): FeedbackResponse
}
