package bose.ankush.weatherify.debug

import bose.ankush.home.domain.ai.AiAvailability
import bose.ankush.home.domain.ai.AiRequest
import bose.ankush.home.domain.ai.OnDeviceAiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Uses the real client whenever the device can run Gemini Nano. Otherwise it reports itself
 * available and streams clearly labelled demo text, so the card, the loading animation, the
 * streaming and the tip chips can be seen on emulators and unsupported phones. The advice in the
 * demo text is the rules' real output, copied from the system instruction; the sentences are not.
 */
internal class DemoFallbackAiClient(
    private val real: OnDeviceAiClient,
) : OnDeviceAiClient {
    override suspend fun availability(): AiAvailability {
        val actual = real.availability()
        return if (actual == AiAvailability.Unavailable) AiAvailability.Available else actual
    }

    override fun generate(request: AiRequest): Flow<String> =
        flow {
            if (real.availability() != AiAvailability.Unavailable) {
                emitAll(real.generate(request))
            } else {
                delay(LOADING_MILLIS)
                var text = ""
                demoText(request).split(" ").forEach { word ->
                    delay(WORD_MILLIS)
                    text = if (text.isEmpty()) word else "$text $word"
                    emit(text)
                }
            }
        }

    private fun demoText(request: AiRequest): String {
        val advice =
            request.systemInstruction
                .lines()
                .filter { it.startsWith("- ") }
                .joinToString(" ") { it.removePrefix("- ").substringBefore(" (") + "." }
        return "[Demo text: this device has no Gemini Nano.] " + advice.ifEmpty { "No special advice today." }
    }
}

private const val LOADING_MILLIS = 1_500L
private const val WORD_MILLIS = 90L
