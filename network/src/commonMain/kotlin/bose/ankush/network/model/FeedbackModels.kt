package bose.ankush.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbackRequest(
    @SerialName("deviceId") val deviceId: String,
    @SerialName("deviceOs") val deviceOs: String,
    @SerialName("feedbackTitle") val feedbackTitle: String,
    @SerialName("feedbackDescription") val feedbackDescription: String,
)

@Serializable
data class FeedbackResponse(
    val success: Boolean = false,
    val data: String? = null, // feedback id
    val message: String? = null,
)
