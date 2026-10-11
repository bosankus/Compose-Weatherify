package bose.ankush.home.data.ai

import bose.ankush.home.domain.ai.AiAvailability
import bose.ankush.home.domain.ai.AiRequest
import bose.ankush.home.domain.ai.AiUnavailableException
import bose.ankush.home.domain.ai.OnDeviceAiClient
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Gemini Nano through ML Kit's Prompt API. Adds up the streamed pieces into the whole text so far,
 * downloads the model first if it is missing, and folds the system instruction into the prompt on
 * devices whose Nano is too old to take one. The SDK sits behind [NanoModel], so this runs in tests.
 * [modelProvider] is only called on first use, so nothing starts at app launch.
 */
internal class MlKitOnDeviceAiClient(
    modelProvider: () -> NanoModel,
) : OnDeviceAiClient {
    private val model by lazy(modelProvider)

    override suspend fun availability(): AiAvailability =
        try {
            model.availability().also { Timber.i("Gemini Nano availability: %s", it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // GenAiException carries AICore's own code (e.g. 601 binding failure, 606 feature not found).
            Timber.w(
                e,
                "Gemini Nano status check failed (AICore error code: %s)",
                (e as? GenAiException)?.errorCode
            )
            AiAvailability.Unavailable
        }

    override fun generate(request: AiRequest): Flow<String> =
        flow {
            makeSureModelIsOnDevice()
            val separate = model.supportsSystemInstruction()
            val system = request.systemInstruction.takeIf { separate }
            val prompt =
                if (separate) request.prompt else "${request.systemInstruction}\n\n${request.prompt}"
            var text = ""
            model.stream(system, prompt).collect { piece ->
                text += piece
                emit(text)
            }
        }

    private suspend fun makeSureModelIsOnDevice() {
        when (model.availability()) {
            AiAvailability.Available -> Unit
            AiAvailability.Downloadable -> model.download()
            AiAvailability.Downloading -> waitForDownload()
            AiAvailability.Unavailable -> throw AiUnavailableException()
        }
    }

    /** Another caller started the download; wait for it instead of starting a second one. */
    private suspend fun waitForDownload() {
        repeat(MAX_DOWNLOAD_POLLS) {
            delay(DOWNLOAD_POLL_MILLIS)
            when (model.availability()) {
                AiAvailability.Available -> return
                AiAvailability.Unavailable -> throw AiUnavailableException()
                else -> Unit
            }
        }
        throw AiUnavailableException()
    }
}

/** The real Gemini Nano client. Public so the app's debug build can wrap it with a demo fallback. */
fun createOnDeviceAiClient(): OnDeviceAiClient =
    MlKitOnDeviceAiClient { GenerativeModelAdapter(Generation.getClient()) }

private const val DOWNLOAD_POLL_MILLIS = 1_000L
private const val MAX_DOWNLOAD_POLLS = 180
