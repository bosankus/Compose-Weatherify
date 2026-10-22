package bose.ankush.home.domain.ai

import bose.ankush.home.domain.model.WeatherForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherRulesTest {
    private val rules = WeatherRules()

    @Test
    fun rain_firesOnlyStrictlyAboveFiveMm() {
        assertEquals(emptyList(), advice(forecast(rain = 5.0)))
        assertEquals(listOf(WeatherAdvice.Umbrella), advice(forecast(rain = 5.1)))
    }

    @Test
    fun uv_firesOnlyStrictlyAboveSeven_fromEitherDailyOrCurrent() {
        assertEquals(emptyList(), advice(forecast(dailyUv = 7.0)))
        assertEquals(listOf(WeatherAdvice.Sunscreen), advice(forecast(dailyUv = 7.1)))
        assertEquals(listOf(WeatherAdvice.Sunscreen), advice(forecast(currentUv = 8.0)))
    }

    @Test
    fun wind_usesTheStrongestOfDailyAndCurrentGust() {
        assertEquals(emptyList(), advice(forecast(dailyWind = 10.0)))
        assertEquals(listOf(WeatherAdvice.Wind), advice(forecast(dailyWind = 4.0, gust = 12.0)))
    }

    @Test
    fun jacket_firesBelowTwelveCelsius_usingTheColdestReading() {
        assertEquals(emptyList(), advice(forecast(feelsLikeCelsius = 12.0)))
        assertEquals(listOf(WeatherAdvice.Jacket), advice(forecast(feelsLikeCelsius = 11.9)))
        assertEquals(
            listOf(WeatherAdvice.Jacket),
            advice(forecast(feelsLikeCelsius = 20.0, eveCelsius = 8.0)),
        )
    }

    @Test
    fun missingData_neverFires() {
        assertEquals(emptyList(), rules.evaluate(WeatherForecast(id = 1)))
    }

    @Test
    fun severalRules_comeOutInAFixedOrder() {
        val all = forecast(rain = 9.0, dailyUv = 9.0, gust = 15.0, feelsLikeCelsius = 3.0)
        assertEquals(
            listOf(
                WeatherAdvice.Umbrella,
                WeatherAdvice.Sunscreen,
                WeatherAdvice.Wind,
                WeatherAdvice.Jacket,
            ),
            advice(all),
        )
    }

    @Test
    fun facts_carryTheNumbers() {
        val facts = rules.evaluate(forecast(rain = 7.24, feelsLikeCelsius = -3.4)).map { it.fact }
        assertTrue("7.2 mm" in facts[0], facts[0])
        assertTrue("-3.4 °C" in facts[1], facts[1])
    }

    @Test
    fun customLimits_areUsed() {
        val strict = WeatherRules(WeatherThresholds(rainMm = 1.0))
        assertEquals(
            listOf(WeatherAdvice.Umbrella),
            strict.evaluate(forecast(rain = 1.5)).map { it.advice },
        )
    }

    private fun advice(forecast: WeatherForecast) = rules.evaluate(forecast).map { it.advice }

    private fun forecast(
        rain: Double? = null,
        dailyUv: Double? = null,
        currentUv: Double? = null,
        dailyWind: Double? = null,
        gust: Double? = null,
        feelsLikeCelsius: Double? = null,
        eveCelsius: Double? = null,
    ) = WeatherForecast(
        id = 1,
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
                    temp =
                        eveCelsius?.let {
                            WeatherForecast.Daily.Temp(
                                day = null,
                                eve = it + KELVIN,
                                max = null,
                                min = null,
                                morn = null,
                                night = null,
                            )
                        },
                    uvi = dailyUv,
                    wind_gust = null,
                    wind_speed = dailyWind,
                ),
            ),
        current =
            WeatherForecast.Current(
                clouds = null,
                dt = null,
                feels_like = feelsLikeCelsius?.let { it + KELVIN },
                humidity = null,
                pressure = null,
                sunrise = null,
                sunset = null,
                temp = null,
                uvi = currentUv,
                wind_gust = gust,
                wind_speed = null,
            ),
    )

    private companion object {
        const val KELVIN = 273.15
    }
}
