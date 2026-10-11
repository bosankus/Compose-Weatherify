package bose.ankush.home.presentation.ai

import bose.ankush.home.domain.ai.AiAvailability
import bose.ankush.home.domain.ai.AiRequest
import bose.ankush.home.domain.ai.AiUnavailableException
import bose.ankush.home.domain.ai.OnDeviceAiClient
import bose.ankush.home.domain.ai.SummarizeWeather
import bose.ankush.home.domain.ai.WeatherPromptBuilder
import bose.ankush.home.domain.ai.WeatherRules
import bose.ankush.home.domain.model.WeatherForecast
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AiSummaryViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theCard_isAvailableUnlessTheDeviceCannotRunTheModel() =
        runTest {
            assertTrue(viewModel(FakeClient(availability = AiAvailability.Downloadable)).also { settle() }.state.value.isAvailable)
            assertFalse(viewModel(FakeClient(availability = AiAvailability.Unavailable)).also { settle() }.state.value.isAvailable)
        }

    @Test
    fun opening_goesFromLoadingToAFinishedSummary_withTranslatedTips() =
        runTest {
            val vm = viewModel(FakeClient(snapshots = listOf("It is wet", "It is wet today.")))
            vm.onIntent(AiSummaryIntent.Open(rainy(), place = "Pune"))
            assertEquals(AiSummaryPhase.Loading, vm.state.value.summary.phase)

            settle()
            val summary = vm.state.value.summary
            assertEquals(AiSummaryPhase.Ready, summary.phase)
            assertEquals("It is wet today.", summary.text)
            assertFalse(summary.isStreaming)
            assertEquals("Pune", summary.place)
            assertEquals(
                listOf(AiSummaryTip(AiSummaryTipKind.Umbrella, "label:Umbrella")),
                summary.tips
            )
        }

    @Test
    fun reopeningTheSameForecast_reusesTheFinishedSummary() =
        runTest {
            val client = FakeClient()
            val vm = viewModel(client)
            vm.onIntent(AiSummaryIntent.Open(rainy(), "Pune"))
            settle()
            vm.onIntent(AiSummaryIntent.Dismiss)
            vm.onIntent(AiSummaryIntent.Open(rainy(), "Pune"))
            settle()

            assertEquals(1, client.requests.size)
            assertEquals(AiSummaryPhase.Ready, vm.state.value.summary.phase)
        }

    @Test
    fun aDifferentForecast_isSummarizedAgain() =
        runTest {
            val client = FakeClient()
            val vm = viewModel(client)
            vm.onIntent(AiSummaryIntent.Open(rainy(rain = 7.0), "Pune"))
            settle()
            vm.onIntent(AiSummaryIntent.Open(rainy(rain = 9.0), "Pune"))
            settle()
            assertEquals(2, client.requests.size)
        }

    @Test
    fun closingMidStream_stopsWriting_andReopeningStartsOver() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val client = FakeClient(snapshots = listOf("Half"), holdOpenUntil = gate)
            val vm = viewModel(client)
            vm.onIntent(AiSummaryIntent.Open(rainy(), null))
            settle()
            assertTrue(vm.state.value.summary.isStreaming)

            vm.onIntent(AiSummaryIntent.Dismiss)
            settle()
            gate.complete(Unit)
            vm.onIntent(AiSummaryIntent.Open(rainy(), null))
            settle()
            assertEquals(2, client.requests.size)
        }

    @Test
    fun aFailure_showsFailed_andRetryTriesAgain() =
        runTest {
            val client = FakeClient(failFirst = IllegalStateException("AICore died"))
            val vm = viewModel(client)
            vm.onIntent(AiSummaryIntent.Open(rainy(), null))
            settle()
            assertEquals(AiSummaryPhase.Failed, vm.state.value.summary.phase)

            vm.onIntent(AiSummaryIntent.Retry)
            settle()
            assertEquals(AiSummaryPhase.Ready, vm.state.value.summary.phase)
            assertEquals(2, client.requests.size)
        }

    @Test
    fun theModelDisappearing_failsTheSheet_andHidesTheCard() =
        runTest {
            val vm = viewModel(FakeClient(failFirst = AiUnavailableException()))
            settle()
            assertTrue(vm.state.value.isAvailable)
            vm.onIntent(AiSummaryIntent.Open(rainy(), null))
            settle()
            assertEquals(AiSummaryPhase.Failed, vm.state.value.summary.phase)
            assertFalse(vm.state.value.isAvailable)
        }

    private fun kotlinx.coroutines.test.TestScope.settle() = advanceUntilIdle()

    private fun viewModel(client: FakeClient) =
        AiSummaryViewModel(
            client,
            SummarizeWeather(client, WeatherRules(), WeatherPromptBuilder())
        ) { "label:${it.name}" }

    private fun rainy(rain: Double = 7.0) =
        WeatherForecast(
            id = 1,
            current =
                WeatherForecast.Current(
                    clouds = null,
                    dt = null,
                    feels_like = 290.0,
                    humidity = null,
                    pressure = null,
                    sunrise = null,
                    sunset = null,
                    temp = 291.0,
                    uvi = null,
                    wind_gust = null,
                    wind_speed = null,
                ),
            daily =
                listOf(
                    WeatherForecast.Daily(
                        clouds = null,
                        dew_point = null,
                        dt = null,
                        humidity = null,
                        pressure = null,
                        rain = rain,
                        summary = null,
                        sunrise = null,
                        sunset = null,
                        temp = null,
                        uvi = null,
                        wind_gust = null,
                        wind_speed = null,
                    ),
                ),
        )

    private class FakeClient(
        private val availability: AiAvailability = AiAvailability.Available,
        private val snapshots: List<String> = listOf("Fine."),
        private var failFirst: Throwable? = null,
        private val holdOpenUntil: CompletableDeferred<Unit>? = null,
    ) : OnDeviceAiClient {
        val requests = mutableListOf<AiRequest>()

        override suspend fun availability() = availability

        override fun generate(request: AiRequest): Flow<String> =
            flow {
                requests += request
                failFirst?.let {
                    failFirst = null
                    throw it
                }
                snapshots.forEach { emit(it) }
                holdOpenUntil?.await()
            }
    }
}
