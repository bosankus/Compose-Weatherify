package bose.ankush.home.data.ai

import bose.ankush.home.domain.ai.AiAvailability
import bose.ankush.home.domain.ai.AiRequest
import bose.ankush.home.domain.ai.OnDeviceAiClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * What Swift implements over Apple's Foundation Models. Plain callbacks, because a Kotlin Flow is
 * awkward to build from Swift; [BridgedOnDeviceAiClient] turns them into the shared contract.
 */
interface NativeAiBridge {
    /** 0 unavailable, 1 downloadable, 2 downloading, 3 available. */
    fun availability(): Int

    /**
     * Starts an answer. Calls [onSnapshot] with the whole text so far, then [onDone] once: null on
     * success, or a message on failure. Neither is called after [NativeAiCall.cancel].
     */
    fun generate(
        systemInstruction: String,
        prompt: String,
        onSnapshot: (String) -> Unit,
        onDone: (String?) -> Unit,
    ): NativeAiCall
}

interface NativeAiCall {
    fun cancel()
}

/** The shared [OnDeviceAiClient] for platforms whose model SDK is native Swift (iOS). */
class BridgedOnDeviceAiClient(
    private val bridge: NativeAiBridge,
) : OnDeviceAiClient {
    override suspend fun availability(): AiAvailability =
        when (bridge.availability()) {
            AVAILABLE -> AiAvailability.Available
            DOWNLOADING -> AiAvailability.Downloading
            DOWNLOADABLE -> AiAvailability.Downloadable
            else -> AiAvailability.Unavailable
        }

    override fun generate(request: AiRequest): Flow<String> =
        callbackFlow {
            val call =
                bridge.generate(
                    systemInstruction = request.systemInstruction,
                    prompt = request.prompt,
                    onSnapshot = { trySend(it) },
                    onDone = { error -> close(error?.let { IllegalStateException(it) }) },
                )
            awaitClose { call.cancel() }
        }.conflate() // every emission is the whole text so far, so a slow collector may skip ahead
}

private const val DOWNLOADABLE = 1
private const val DOWNLOADING = 2
private const val AVAILABLE = 3
