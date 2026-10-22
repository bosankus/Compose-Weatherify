package bose.ankush.home.data.ai

import bose.ankush.home.domain.ai.AiAvailability
import bose.ankush.home.domain.ai.AiRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BridgedOnDeviceAiClientTest {
    private val request = AiRequest(systemInstruction = "RULES", prompt = "PROMPT")

    @Test
    fun availabilityCodes_mapToTheSharedStates() =
        runTest {
            val codes =
                mapOf(
                    0 to AiAvailability.Unavailable,
                    1 to AiAvailability.Downloadable,
                    2 to AiAvailability.Downloading,
                    3 to AiAvailability.Available,
                    99 to AiAvailability.Unavailable,
                )
            codes.forEach { (code, expected) ->
                assertEquals(
                    expected,
                    BridgedOnDeviceAiClient(FakeBridge(code)).availability(),
                    "code $code",
                )
            }
        }

    @Test
    fun snapshotsAreForwarded_andTheFlowEndsWhenSwiftIsDone() =
        runTest {
            val bridge =
                FakeBridge(script = { snapshot, done ->
                    snapshot("It")
                    snapshot("It is mild.")
                    done(null)
                })
            val out = BridgedOnDeviceAiClient(bridge).generate(request).toList()
            assertEquals("It is mild.", out.last())
            assertEquals("RULES" to "PROMPT", bridge.lastRequest)
        }

    @Test
    fun aFailureFromSwift_becomesAnExceptionInTheFlow() =
        runTest {
            val bridge = FakeBridge(script = { _, done -> done("guardrail") })
            val e =
                assertFailsWith<IllegalStateException> {
                    BridgedOnDeviceAiClient(bridge).generate(request).toList()
                }
            assertEquals("guardrail", e.message)
        }

    @Test
    fun stopCollecting_cancelsTheSwiftCall() =
        runTest {
            val bridge = FakeBridge(script = { snapshot, _ -> snapshot("first") })
            val first = BridgedOnDeviceAiClient(bridge).generate(request).first()
            assertEquals("first", first)
            assertTrue(bridge.cancelled, "the call into Swift should be cancelled")
        }

    private class FakeBridge(
        private val code: Int = 3,
        private val script: (snapshot: (String) -> Unit, done: (String?) -> Unit) -> Unit = { _, done ->
            done(
                null,
            )
        },
    ) : NativeAiBridge {
        var lastRequest: Pair<String, String>? = null
        var cancelled = false

        override fun availability() = code

        override fun generate(
            systemInstruction: String,
            prompt: String,
            onSnapshot: (String) -> Unit,
            onDone: (String?) -> Unit,
        ): NativeAiCall {
            lastRequest = systemInstruction to prompt
            script(onSnapshot, onDone)
            return object : NativeAiCall {
                override fun cancel() {
                    cancelled = true
                }
            }
        }
    }
}
