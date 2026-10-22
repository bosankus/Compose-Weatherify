package bose.ankush.home.domain.ai

import bose.ankush.home.domain.model.WeatherForecast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SummarizeWeatherTest {
    @Test
    fun streamsTheTextSoFar_thenOneCompleteEmission() =
        runTest {
            val client = FakeClient(listOf("It", "It is mild", "It is mild today."))
            val out = summarize(client).invoke(forecast(rain = 7.0)).toList()

            assertEquals(
                listOf("It", "It is mild", "It is mild today.", "It is mild today."),
                out.map { it.text },
            )
            assertEquals(listOf(false, false, false, true), out.map { it.isComplete })
            assertEquals(listOf(WeatherAdvice.Umbrella), out.last().findings.map { it.advice })
        }

    @Test
    fun theRequestCarriesTheRulesFindings() =
        runTest {
            val client = FakeClient(listOf("ok"))
            summarize(client).invoke(forecast(rain = 7.0), place = "Pune").toList()

            val request = client.requests.single()
            assertTrue("Take an umbrella" in request.systemInstruction)
            assertTrue("Place: Pune." in request.prompt)
        }

    @Test
    fun blankAnswer_isAnError() =
        runTest {
            assertFailsWith<EmptySummaryException> {
                summarize(FakeClient(listOf("  ")))
                    .invoke(
                        forecast(),
                    ).toList()
            }
        }

    @Test
    fun noForecastData_isAnError_andNeverReachesTheModel() =
        runTest {
            val client = FakeClient(listOf("x"))
            assertFailsWith<IllegalArgumentException> {
                summarize(client).invoke(WeatherForecast(id = 1)).toList()
            }
            assertTrue(client.requests.isEmpty())
        }

    @Test
    fun aPlatformFailure_passesThrough() =
        runTest {
            val client = FakeClient(emptyList(), failure = IllegalStateException("AICore died"))
            assertFailsWith<IllegalStateException> { summarize(client).invoke(forecast()).toList() }
        }

    private fun summarize(client: OnDeviceAiClient) = SummarizeWeather(client, WeatherRules(), WeatherPromptBuilder())

    private fun forecast(rain: Double? = null) =
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
        private val snapshots: List<String>,
        private val failure: Throwable? = null,
    ) : OnDeviceAiClient {
        val requests = mutableListOf<AiRequest>()

        override suspend fun availability() = AiAvailability.Available

        override fun generate(request: AiRequest): Flow<String> =
            flow {
                requests += request
                snapshots.forEach { emit(it) }
                failure?.let { throw it }
            }
    }
}
