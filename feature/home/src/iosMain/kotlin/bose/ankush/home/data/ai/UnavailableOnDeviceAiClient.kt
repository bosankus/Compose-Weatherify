package bose.ankush.home.data.ai

import bose.ankush.home.domain.ai.AiAvailability
import bose.ankush.home.domain.ai.AiRequest
import bose.ankush.home.domain.ai.AiUnavailableException
import bose.ankush.home.domain.ai.OnDeviceAiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Placeholder until the Swift Foundation Models client is handed in: the feature stays hidden. */
internal class UnavailableOnDeviceAiClient : OnDeviceAiClient {
    override suspend fun availability(): AiAvailability = AiAvailability.Unavailable

    override fun generate(request: AiRequest): Flow<String> =
        flow { throw AiUnavailableException() }
}
