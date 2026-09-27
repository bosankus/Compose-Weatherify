package bose.ankush.storage

import bose.ankush.storage.model.AirQualityData
import bose.ankush.storage.model.WeatherCondition
import bose.ankush.storage.model.WeatherData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeatherEntityMapperTest {
    @Test
    fun weatherData_roundTripsThroughEntity() {
        val original =
            WeatherData(
                id = 42L,
                alerts =
                    listOf(
                        WeatherData.Alert(
                            description = "Storm",
                            end = 2L,
                            event = "Wind",
                            sender_name = "NWS",
                            start = 1L,
                        ),
                    ),
                current =
                    WeatherData.Current(
                        clouds = 10,
                        dt = 100L,
                        feels_like = 25.5,
                        humidity = 40,
                        pressure = 1010,
                        sunrise = 1L,
                        sunset = 2L,
                        temp = 26.0,
                        uvi = 3.0,
                        weather = listOf(WeatherCondition("clear sky", "01d", 800, "Clear")),
                        wind_gust = 5.0,
                        wind_speed = 3.0,
                    ),
                daily =
                    listOf(
                        WeatherData.Daily(
                            clouds = 5,
                            dew_point = 10.0,
                            dt = 200L,
                            humidity = 30,
                            pressure = 1005,
                            rain = 0.0,
                            summary = "Sunny",
                            sunrise = 1L,
                            sunset = 2L,
                            temp =
                                WeatherData.Daily.Temp(
                                    day = 26.0,
                                    eve = 22.0,
                                    max = 28.0,
                                    min = 18.0,
                                    morn = 19.0,
                                    night = 20.0,
                                ),
                            uvi = 4.0,
                            weather = listOf(WeatherCondition("clear sky", "01d", 800, "Clear")),
                            wind_gust = 4.0,
                            wind_speed = 2.0,
                        ),
                    ),
                hourly =
                    listOf(
                        WeatherData.Hourly(
                            clouds = 0,
                            dt = 300L,
                            feels_like = 24.0,
                            humidity = 35,
                            temp = 25.0,
                            weather = listOf(WeatherCondition("clear sky", "01d", 800, "Clear")),
                        ),
                    ),
                lastUpdated = 999L,
            )

        val roundTripped = original.toWeatherEntity().toWeatherData()
        assertEquals(original, roundTripped)
    }

    @Test
    fun airQualityData_roundTripsThroughEntity() {
        val original =
            AirQualityData(
                id = 7L,
                aqi = 2,
                co = 1.1,
                no2 = 2.2,
                o3 = 3.3,
                so2 = 4.4,
                pm10 = 5.5,
                pm25 = 6.6,
            )
        assertEquals(original, original.toAirQualityEntity().toAirQualityData())
    }

    @Test
    fun emptyWeather_roundTripsNullNestedFields() {
        val original = WeatherData(id = 1L)
        val roundTripped = original.toWeatherEntity().toWeatherData()
        assertEquals(1L, roundTripped.id)
        assertNull(roundTripped.current)
        assertEquals(0L, roundTripped.lastUpdated)
    }
}
