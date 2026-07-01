package bose.ankush.weatherify.domain.use_case

import bose.ankush.network.model.FeedbackRequest
import bose.ankush.network.model.FeedbackResponse
import bose.ankush.network.repository.FeedbackRepository
import javax.inject.Inject

class SubmitFeedbackUseCase
    @Inject
    constructor(
        private val repository: FeedbackRepository,
    ) {
        suspend operator fun invoke(request: FeedbackRequest): Result<FeedbackResponse> =
            repository.submitFeedback(request)
    }
