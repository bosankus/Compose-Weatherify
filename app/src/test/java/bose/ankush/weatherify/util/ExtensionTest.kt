package bose.ankush.weatherify.util

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import bose.ankush.weatherify.data.model.WeatherForecast
import bose.ankush.weatherify.util.Extension.getDayName
import bose.ankush.weatherify.util.Extension.getForecastListForNext4Days
import bose.ankush.weatherify.util.Extension.toCelsius
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class ExtensionTest {

    private lateinit var forecastList: List<WeatherForecast.ForecastList>

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        // read sample file
        val inputStream = File("src/test/data.txt").inputStream()
        val stringFile = inputStream.bufferedReader().use { it.readText() }
        // convert string file to forecast list
        val weatherForecast: WeatherForecast =
            Gson().fromJson(stringFile, WeatherForecast::class.java)
        forecastList = weatherForecast.list!!
    }

    @Test
    fun toCelsius_WhenFahrenheitTempProvided_ReturnsCelsiusString() {
        // test temp is 87.0F, output 31C
        val farTemp = 87.0
        val celsiusTemp = farTemp.toCelsius()
        assertThat(celsiusTemp).isEqualTo("31")
    }

    @Test
    fun getForecastListForNext4Days() {
        val nextDayName: String? = forecastList.getForecastListForNext4Days()[0].nameOfDay
        assertThat(nextDayName).isEqualTo("SATURDAY")
    }

    @Test
    fun getDayName_WhenProvidedEpochTimestamp_ReturnsDayName() {
        // test UTC time is 1631113200
        val sampleIntEpoch = 1631113200
        val dayName = sampleIntEpoch.getDayName()

        // assert the specific day was Wednesday
        assertThat(dayName).isEqualTo("WEDNESDAY")
    }
}