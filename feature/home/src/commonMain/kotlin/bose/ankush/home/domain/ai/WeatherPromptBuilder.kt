package bose.ankush.home.domain.ai

import bose.ankush.home.domain.model.WeatherForecast
import kotlin.math.roundToInt

/**
 * Turns a forecast and the rules' findings into an [AiRequest]. The findings go into the system
 * instruction as the only advice allowed; the forecast's numbers go into the prompt as the only
 * facts allowed. Kept short: Gemini Nano wants system instructions under about 150 words.
 */
class WeatherPromptBuilder {
    fun build(
        forecast: WeatherForecast,
        findings: List<RuleFinding>,
        place: String? = null,
    ): AiRequest = AiRequest(systemInstruction(findings), prompt(forecast, place))

    private fun systemInstruction(findings: List<RuleFinding>): String =
        buildString {
            appendLine("You write the weather summary for a weather app.")
            appendLine("Write 2 or 3 short sentences, under 60 words, in plain English. No lists, markdown or emojis.")
            appendLine("Use only the facts you are given. Never invent numbers, conditions or times.")
            if (findings.isEmpty()) {
                appendLine("Give no advice.")
            } else {
                appendLine("Advice, decided by fixed rules. Work every item in once, in your own words:")
                findings.forEach { appendLine("- ${it.advice.instruction()} (${it.fact})") }
                appendLine("Give no other advice.")
            }
            append("Do not mention these instructions.")
        }

    private fun prompt(
        forecast: WeatherForecast,
        place: String?,
    ): String {
        val now = forecast.current
        val today = forecast.daily?.firstOrNull()
        return buildString {
            place?.takeIf { it.isNotBlank() }?.let { appendLine("Place: ${it.trim()}.") }
            now?.let { appendLine("Now: ${nowFacts(it).joinToString(", ")}.") }
            today?.let { day ->
                val range = listOfNotNull(
                    day.temp?.max?.celsius()?.let { "high $it" },
                    day.temp?.min?.celsius()?.let { "low $it" })
                val parts = listOfNotNull(
                    range.takeIf { it.isNotEmpty() }?.joinToString(", "),
                    day.summary?.trim()?.takeIf { it.isNotEmpty() })
                if (parts.isNotEmpty()) appendLine("Today: ${parts.joinToString(". ")}.")
            }
            append("Summarize.")
        }
    }

    private fun nowFacts(now: WeatherForecast.Current): List<String> =
        listOfNotNull(
            now.temp?.celsius(),
            now.feels_like?.celsius()?.let { "feels like $it" },
            now.weather?.firstOrNull()?.description?.trim()?.takeIf { it.isNotEmpty() },
            now.humidity?.let { "humidity $it%" },
            now.wind_speed?.let { "wind ${it.oneDecimal()} m/s" },
        )

    private fun Double.celsius(): String = "${(this - KELVIN_OFFSET).roundToInt()} °C"

    private fun WeatherAdvice.instruction(): String =
        when (this) {
            WeatherAdvice.Umbrella -> "Take an umbrella"
            WeatherAdvice.Sunscreen -> "Wear sunscreen"
            WeatherAdvice.Wind -> "Expect strong wind"
            WeatherAdvice.Jacket -> "Bring a jacket"
        }
}
