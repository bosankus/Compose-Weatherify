package bose.ankush.home.domain.ai

import bose.ankush.home.domain.model.WeatherCondition
import bose.ankush.home.domain.model.WeatherForecast
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeatherPromptBuilderTest {
    private val builder = WeatherPromptBuilder()

    @Test
    fun noFindings_saysGiveNoAdvice() {
        val request = builder.build(forecast(), emptyList())
        assertTrue("Give no advice." in request.systemInstruction)
        assertFalse("Take an umbrella" in request.systemInstruction)
    }

    @Test
    fun findings_areTheOnlyAdviceAndCarryTheirFacts() {
        val findings =
            listOf(
                RuleFinding(WeatherAdvice.Umbrella, "Rain expected today: 7.2 mm."),
                RuleFinding(WeatherAdvice.Jacket, "Feels as cold as 3.0 °C today."),
            )
        val system = builder.build(forecast(), findings).systemInstruction
        assertTrue("- Take an umbrella (Rain expected today: 7.2 mm.)" in system, system)
        assertTrue("- Bring a jacket (Feels as cold as 3.0 °C today.)" in system, system)
        assertTrue("Give no other advice." in system)
        assertFalse("Wear sunscreen" in system)
    }

    @Test
    fun systemInstruction_staysUnder150Words_evenWithEveryRule() {
        val all = WeatherAdvice.entries.map { RuleFinding(it, "Rain expected today: 12.5 mm.") }
        val words =
            builder
                .build(forecast(), all)
                .systemInstruction
                .split(Regex("\\s+"))
                .size
        assertTrue(words < 150, "was $words words")
    }

    @Test
    fun prompt_hasTheForecastsOwnNumbers_inCelsius() {
        val prompt = builder.build(forecast(), emptyList(), place = " Mountain View ").prompt
        assertTrue("Place: Mountain View." in prompt, prompt)
        assertTrue(
            "Now: 13 °C, feels like 12 °C, scattered clouds, humidity 91%, wind 1.0 m/s." in prompt,
            prompt,
        )
        assertTrue("Today: high 28 °C, low 9 °C. Clear spells." in prompt, prompt)
    }

    @Test
    fun missingFields_areLeftOut_notGuessed() {
        val prompt =
            builder
                .build(
                    WeatherForecast(id = 1, current = current(temp = null)),
                    emptyList(),
                ).prompt
        assertFalse("Place" in prompt)
        assertFalse("Today" in prompt)
        assertFalse("null" in prompt)
    }

    private fun forecast() =
        WeatherForecast(
            id = 1,
            current = current(),
            daily =
                listOf(
                    WeatherForecast.Daily(
                        clouds = null,
                        dew_point = null,
                        dt = null,
                        humidity = null,
                        pressure = null,
                        rain = null,
                        summary = "Clear spells",
                        sunrise = null,
                        sunset = null,
                        temp =
                            WeatherForecast.Daily.Temp(
                                day = null,
                                eve = null,
                                max = 301.15,
                                min = 282.15,
                                morn = null,
                                night = null,
                            ),
                        uvi = null,
                        wind_gust = null,
                        wind_speed = null,
                    ),
                ),
        )

    private fun current(temp: Double? = 286.15) =
        WeatherForecast.Current(
            clouds = null,
            dt = null,
            feels_like = 285.15,
            humidity = 91,
            pressure = null,
            sunrise = null,
            sunset = null,
            temp = temp,
            uvi = null,
            weather = listOf(WeatherCondition(description = "scattered clouds", id = 802)),
            wind_gust = null,
            wind_speed = 0.96,
        )
}
