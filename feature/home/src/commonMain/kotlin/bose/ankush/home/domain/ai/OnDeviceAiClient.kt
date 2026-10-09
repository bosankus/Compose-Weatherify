package bose.ankush.home.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * The one platform-specific piece of on-device AI: a prompt in, streamed text out.
 * Android wraps ML Kit (Gemini Nano); iOS wraps Foundation Models from Swift.
 */
interface OnDeviceAiClient {
    /** Whether the model can answer right now. */
    suspend fun availability(): AiAvailability

    /** Streams the answer. Each emission is the whole text so far, not a piece of it. */
    fun generate(request: AiRequest): Flow<String>
}

/** [systemInstruction] is what the model must follow; [prompt] is what it answers. */
data class AiRequest(
    val systemInstruction: String,
    val prompt: String,
)

enum class AiAvailability {
    Available,

    /** Supported, but the model is not on the device yet. */
    Downloadable,
    Downloading,

    /** Not supported or not usable on this device. */
    Unavailable,
}

/** [OnDeviceAiClient.generate] was called while the model cannot be used. */
class AiUnavailableException : Exception("On-device AI is not available.")
