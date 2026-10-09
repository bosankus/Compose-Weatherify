package bose.ankush.home.domain.ai

import bose.ankush.home.domain.model.WeatherForecast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** The summary so far. [findings] are the rules' advice, known before the first word arrives. */
data class WeatherSummary(
    val text: String,
    val findings: List<RuleFinding>,
    val isComplete: Boolean,
)

class EmptySummaryException : Exception("The model returned no text.")

/**
 * Runs the rules, builds the prompt, and streams the model's answer. Same shape as the other
 * home use cases; the ViewModel decides when to call it and maps the result to UI state.
 * A failure (no data, empty answer, a platform error) arrives as an exception from the flow.
 */
class SummarizeWeather(
    private val client: OnDeviceAiClient,
    private val rules: WeatherRules,
    private val promptBuilder: WeatherPromptBuilder,
) {
    operator fun invoke(
        forecast: WeatherForecast,
        place: String? = null,
    ): Flow<WeatherSummary> =
        flow {
            require(forecast.current != null || !forecast.daily.isNullOrEmpty()) { "No forecast data to summarize." }
            val findings = rules.evaluate(forecast)
            val request = promptBuilder.build(forecast, findings, place)
            var text = ""
            client.generate(request).collect {
                text = it.trim()
                emit(WeatherSummary(text, findings, isComplete = false))
            }
            if (text.isEmpty()) throw EmptySummaryException()
            emit(WeatherSummary(text, findings, isComplete = true))
        }
}
