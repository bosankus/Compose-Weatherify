package bose.ankush.network.repository

import bose.ankush.network.model.FeedbackRequest
import bose.ankush.network.model.FeedbackResponse

/**
 * Repository interface for feedback operations
 */
interface FeedbackRepository {
    /**
     * Submit user feedback
     * Returns a Result wrapping either the response or an error
     */
    suspend fun submitFeedback(request: FeedbackRequest): Result<FeedbackResponse>
}