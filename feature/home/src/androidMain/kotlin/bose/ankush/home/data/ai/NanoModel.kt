package bose.ankush.home.data.ai

import bose.ankush.home.domain.ai.AiAvailability
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/** The few ML Kit calls the client needs, so the client's logic can be tested without the SDK. */
internal interface NanoModel {
    suspend fun availability(): AiAvailability

    /** Returns when the model is on the device; throws if the download fails. */
    suspend fun download()

    suspend fun supportsSystemInstruction(): Boolean

    /** Streams answer pieces, not the text so far. A null [system] means none is sent. */
    fun stream(
        system: String?,
        prompt: String,
    ): Flow<String>
}

/** The real thing: a thin pass-through to ML Kit's [GenerativeModel]. Needs a supported device. */
internal class GenerativeModelAdapter(
    private val model: GenerativeModel,
) : NanoModel {
    override suspend fun availability(): AiAvailability {
        val status = model.checkStatus()
        // The raw answer from AICore, so "why no card?" can be read off logcat.
        Timber.i("ML Kit GenAI feature status: %s", status.featureStatusName())
        return when (status) {
            FeatureStatus.AVAILABLE -> AiAvailability.Available
            FeatureStatus.DOWNLOADABLE -> AiAvailability.Downloadable
            FeatureStatus.DOWNLOADING -> AiAvailability.Downloading
            else -> AiAvailability.Unavailable
        }
    }

    private fun Int.featureStatusName(): String =
        when (this) {
            FeatureStatus.AVAILABLE -> "AVAILABLE"
            FeatureStatus.DOWNLOADABLE -> "DOWNLOADABLE"
            FeatureStatus.DOWNLOADING -> "DOWNLOADING"
            FeatureStatus.UNAVAILABLE -> "UNAVAILABLE"
            else -> "UNKNOWN($this)"
        }

    override suspend fun download() {
        model.download().collect { status ->
            if (status is DownloadStatus.DownloadFailed) throw status.e
        }
    }

    override suspend fun supportsSystemInstruction(): Boolean = model.isSystemPromptAvailable()

    override fun stream(
        system: String?,
        prompt: String,
    ): Flow<String> {
        val request =
            if (system != null) {
                generateContentRequest(SystemInstruction(system), TextPart(prompt)) { tune() }
            } else {
                generateContentRequest(TextPart(prompt)) { tune() }
            }
        return model
            .generateContentStream(request)
            .map {
                it.candidates
                    .firstOrNull()
                    ?.text
                    .orEmpty()
            }
    }

    private fun com.google.mlkit.genai.prompt.GenerateContentRequest.Builder.tune() {
        // Facts, not flair: low temperature. About 60 words is about 90 tokens; leave some room.
        temperature = SUMMARY_TEMPERATURE
        maxOutputTokens = SUMMARY_MAX_TOKENS
    }
}

private const val SUMMARY_TEMPERATURE = 0.3f
private const val SUMMARY_MAX_TOKENS = 120
