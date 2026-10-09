package bose.ankush.home.domain.ai

import bose.ankush.home.domain.model.WeatherForecast
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WeatherAdvice { Umbrella, Sunscreen, Wind, Jacket }

/** A rule that fired. [fact] is plain English for the prompt; the UI words its own chip. */
data class RuleFinding(
    val advice: WeatherAdvice,
    val fact: String,
)

/** Rain, UV and wind trigger strictly above their limit; a jacket strictly below its. */
data class WeatherThresholds(
    val rainMm: Double = 5.0,
    val uvIndex: Double = 7.0,
    val windMetersPerSecond: Double = 10.0,
    val jacketBelowCelsius: Double = 12.0,
)

/**
 * Decides what to advise, with no model involved, so the model only phrases it and can never
 * invent it. Today is the first daily entry. Missing data never triggers a rule.
 * Forecast temperatures are Kelvin, wind m/s, rain mm.
 */
class WeatherRules(
    private val limits: WeatherThresholds = WeatherThresholds(),
) {
    fun evaluate(forecast: WeatherForecast): List<RuleFinding> {
        val today = forecast.daily?.firstOrNull()
        val now = forecast.current
        return listOfNotNull(
            umbrella(today?.rain),
            sunscreen(listOfNotNull(today?.uvi, now?.uvi).maxOrNull()),
            wind(listOfNotNull(today?.wind_speed, now?.wind_speed, now?.wind_gust).maxOrNull()),
            jacket(listOfNotNull(now?.feels_like, today?.temp?.morn, today?.temp?.eve).minOrNull()),
        )
    }

    private fun umbrella(rainMm: Double?): RuleFinding? =
        rainMm?.takeIf { it > limits.rainMm }?.let {
            RuleFinding(WeatherAdvice.Umbrella, "Rain expected today: ${it.oneDecimal()} mm.")
        }

    private fun sunscreen(uv: Double?): RuleFinding? =
        uv?.takeIf { it > limits.uvIndex }?.let {
            RuleFinding(WeatherAdvice.Sunscreen, "UV index reaches ${it.oneDecimal()} today.")
        }

    private fun wind(metersPerSecond: Double?): RuleFinding? =
        metersPerSecond?.takeIf { it > limits.windMetersPerSecond }?.let {
            RuleFinding(WeatherAdvice.Wind, "Wind up to ${it.oneDecimal()} m/s today.")
        }

    private fun jacket(coldestKelvin: Double?): RuleFinding? =
        coldestKelvin?.let { it - KELVIN_OFFSET }?.takeIf { it < limits.jacketBelowCelsius }?.let {
            RuleFinding(WeatherAdvice.Jacket, "Feels as cold as ${it.oneDecimal()} °C today.")
        }
}

internal const val KELVIN_OFFSET = 273.15

internal fun Double.oneDecimal(): String {
    val tenths = (this * 10).roundToInt()
    val sign = if (tenths < 0) "-" else ""
    return "$sign${abs(tenths) / 10}.${abs(tenths) % 10}"
}
