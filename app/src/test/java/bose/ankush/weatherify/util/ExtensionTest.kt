package bose.ankush.weatherify.util

import bose.ankush.weatherify.model.model.WeatherForecast
import bose.ankush.weatherify.util.Extension.getForecastListForNext4Days
import bose.ankush.weatherify.util.Extension.toCelsius
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class ExtensionTest {

    @Test
    fun toCelsius_WhenKelvinTempProvided_ReturnsCelsiusString() {
        // test temp is 303.12K, output 30C
        val farTemp = 303.12
        val celsiusTemp = farTemp.toCelsius()
        assertThat(celsiusTemp).isEqualTo("30")
    }

    @Test
    fun getForecastListForNext4Days_WhenProvidedForecastList_ReturnsNextDayName() {
        // read sample file
        val inputStream = File("src/test/forecast.json").inputStream()
        val stringFile = inputStream.bufferedReader().use { it.readText() }
        // convert string file to forecast list
        val weatherForecast: WeatherForecast =
            Gson().fromJson(stringFile, WeatherForecast::class.java)
        val forecastList = weatherForecast.list!!

        // finding next day name from read file
        val nextDayName: String? = forecastList.getForecastListForNext4Days()[0].nameOfDay

        // before assertion, don't forget to update 'forecast.json' file,
        // and check respective next day name for successful test
        assertThat(nextDayName).isEqualTo("TUESDAY")
    }

}