package bose.ankush.home.data.ai

import bose.ankush.home.domain.ai.AiAvailability
import bose.ankush.home.domain.ai.AiRequest
import bose.ankush.home.domain.ai.AiUnavailableException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MlKitOnDeviceAiClientTest {
    private val request = AiRequest(systemInstruction = "RULES", prompt = "PROMPT")

    @Test
    fun streamedPieces_areAddedUpIntoTheTextSoFar() =
        runTest {
            val model = FakeModel(pieces = listOf("Hel", "lo", " world"))
            val out = client(model).generate(request).toList()
            assertEquals(listOf("Hel", "Hello", "Hello world"), out)
        }

    @Test
    fun whenSystemInstructionsAreSupported_theyTravelSeparately() =
        runTest {
            val model = FakeModel(systemSupported = true)
            client(model).generate(request).toList()
            assertEquals("RULES", model.lastSystem)
            assertEquals("PROMPT", model.lastPrompt)
        }

    @Test
    fun whenTheyAreNot_theyAreFoldedIntoThePrompt() =
        runTest {
            val model = FakeModel(systemSupported = false)
            client(model).generate(request).toList()
            assertNull(model.lastSystem)
            assertEquals("RULES\n\nPROMPT", model.lastPrompt)
        }

    @Test
    fun aMissingModel_isDownloadedBeforeAnyGeneration() =
        runTest {
            val model = FakeModel(statuses = mutableListOf(AiAvailability.Downloadable))
            client(model).generate(request).toList()
            assertEquals(listOf("download", "stream"), model.calls)
        }

    @Test
    fun aModelAlreadyDownloading_isWaitedFor_notDownloadedTwice() =
        runTest {
            val model =
                FakeModel(
                    statuses =
                        mutableListOf(
                            AiAvailability.Downloading,
                            AiAvailability.Downloading,
                            AiAvailability.Available
                        ),
                )
            client(model).generate(request).toList()
            assertEquals(listOf("stream"), model.calls)
        }

    @Test
    fun anUnavailableModel_failsWithoutStreaming() =
        runTest {
            val model = FakeModel(statuses = mutableListOf(AiAvailability.Unavailable))
            assertFailsWith<AiUnavailableException> { client(model).generate(request).toList() }
            assertEquals(emptyList(), model.calls)
        }

    @Test
    fun aFailedDownload_passesThrough() =
        runTest {
            val model =
                FakeModel(
                    statuses = mutableListOf(AiAvailability.Downloadable),
                    downloadFailure = IllegalStateException("net")
                )
            assertFailsWith<IllegalStateException> { client(model).generate(request).toList() }
        }

    @Test
    fun availability_mapsTheModel_andTurnsSdkErrorsIntoUnavailable() =
        runTest {
            assertEquals(
                AiAvailability.Downloadable,
                client(FakeModel(statuses = mutableListOf(AiAvailability.Downloadable))).availability()
            )
            assertEquals(
                AiAvailability.Unavailable,
                client(FakeModel(statusFailure = IllegalStateException("601"))).availability()
            )
        }

    private fun client(model: NanoModel) = MlKitOnDeviceAiClient { model }

    private class FakeModel(
        private val pieces: List<String> = listOf("ok"),
        private val systemSupported: Boolean = true,
        /** Answers to successive status checks; the last one repeats. */
        private val statuses: MutableList<AiAvailability> = mutableListOf(AiAvailability.Available),
        private val downloadFailure: Throwable? = null,
        private val statusFailure: Throwable? = null,
    ) : NanoModel {
        val calls = mutableListOf<String>()
        var lastSystem: String? = "unset"
        var lastPrompt: String? = null

        override suspend fun availability(): AiAvailability {
            statusFailure?.let { throw it }
            return if (statuses.size > 1) statuses.removeAt(0) else statuses.first()
        }

        override suspend fun download() {
            calls += "download"
            downloadFailure?.let { throw it }
            statuses.clear()
            statuses += AiAvailability.Available
        }

        override suspend fun supportsSystemInstruction() = systemSupported

        override fun stream(
            system: String?,
            prompt: String,
        ): Flow<String> {
            calls += "stream"
            lastSystem = system
            lastPrompt = prompt
            return flowOf(*pieces.toTypedArray())
        }
    }
}
