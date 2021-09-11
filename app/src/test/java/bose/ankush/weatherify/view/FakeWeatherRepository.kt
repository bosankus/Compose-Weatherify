package bose.ankush.weatherify.view

import bose.ankush.weatherify.data.WeatherRepository
import bose.ankush.weatherify.data.model.CurrentTemperature
import bose.ankush.weatherify.data.model.WeatherForecast
import com.google.gson.Gson
import java.io.File

class FakeWeatherRepository : WeatherRepository {

    override suspend fun getCurrentTemperature(): CurrentTemperature? {
        val stringFile = convertJsonFileToString("src/test/temperature.txt")
        return Gson().fromJson(stringFile, CurrentTemperature::class.java)
    }

    override suspend fun getWeatherForecast(): WeatherForecast? {
        val stringFile = convertJsonFileToString("src/test/forecast.txt")
        return Gson().fromJson(stringFile, WeatherForecast::class.java)
    }

    private fun convertJsonFileToString(pathName: String): String {
        val inputStream = File(pathName).inputStream()
        return inputStream.bufferedReader().use { it.readText() }
    }
}