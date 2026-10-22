package bose.ankush.home.domain.ai

import bose.ankush.home.domain.model.WeatherForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeatherRulesTest {
    private val rules = WeatherRules()

    @Test
    fun rain_firesOnlyStrictlyAboveFiveMm() {
        assertEquals(emptyList(), advice(Readings(rain = 5.0).forecast()))
        assertEquals(listOf(WeatherAdvice.Umbrella), advice(Readings(rain = 5.1).forecast()))
    }

    @Test
    fun uv_firesOnlyStrictlyAboveSeven_fromEitherDailyOrCurrent() {
        assertEquals(emptyList(), advice(Readings(dailyUv = 7.0).forecast()))
        assertEquals(listOf(WeatherAdvice.Sunscreen), advice(Readings(dailyUv = 7.1).forecast()))
        assertEquals(listOf(WeatherAdvice.Sunscreen), advice(Readings(currentUv = 8.0).forecast()))
    }

    @Test
    fun wind_usesTheStrongestOfDailyAndCurrentGust() {
        assertEquals(emptyList(), advice(Readings(dailyWind = 10.0).forecast()))
        assertEquals(listOf(WeatherAdvice.Wind), advice(Readings(dailyWind = 4.0, gust = 12.0).forecast()))
    }

    @Test
    fun jacket_firesBelowTwelveCelsius_usingTheColdestReading() {
        assertEquals(emptyList(), advice(Readings(feelsLikeCelsius = 12.0).forecast()))
        assertEquals(listOf(WeatherAdvice.Jacket), advice(Readings(feelsLikeCelsius = 11.9).forecast()))
        assertEquals(
            listOf(WeatherAdvice.Jacket),
            advice(Readings(feelsLikeCelsius = 20.0, eveCelsius = 8.0).forecast()),
        )
    }

    @Test
    fun missingData_neverFires() {
        assertEquals(emptyList(), rules.evaluate(WeatherForecast(id = 1)))
    }

    @Test
    fun severalRules_comeOutInAFixedOrder() {
        val all = Readings(rain = 9.0, dailyUv = 9.0, gust = 15.0, feelsLikeCelsius = 3.0).forecast()
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
        val facts = rules.evaluate(Readings(rain = 7.24, feelsLikeCelsius = -3.4).forecast()).map { it.fact }
        assertTrue("7.2 mm" in facts[0], facts[0])
        assertTrue("-3.4 °C" in facts[1], facts[1])
    }

    @Test
    fun customLimits_areUsed() {
        val strict = WeatherRules(WeatherThresholds(rainMm = 1.0))
        assertEquals(
            listOf(WeatherAdvice.Umbrella),
            strict.evaluate(Readings(rain = 1.5).forecast()).map { it.advice },
        )
    }

    private fun advice(forecast: WeatherForecast) = rules.evaluate(forecast).map { it.advice }

    /** The readings a test sets; everything else in the forecast stays null. */
    private data class Readings(
        val rain: Double? = null,
        val dailyUv: Double? = null,
        val currentUv: Double? = null,
        val dailyWind: Double? = null,
        val gust: Double? = null,
        val feelsLikeCelsius: Double? = null,
        val eveCelsius: Double? = null,
    ) {
        fun forecast() =
            WeatherForecast(
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
    }

    private companion object {
        const val KELVIN = 273.15
    }
}
