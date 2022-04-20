package bose.ankush.weatherify.model.repository

import androidx.test.platform.app.InstrumentationRegistry
import bose.ankush.weatherify.WeatherifyApplication
import okio.IOException
import java.io.InputStreamReader

object FileReader {

    fun readStringFromFile(fileName: String): String {
        try {
            val inputStream = (InstrumentationRegistry.getInstrumentation().targetContext
                .applicationContext as WeatherifyApplication).assets.open(fileName)
            val builder = StringBuilder()
            val reader = InputStreamReader(inputStream, "UTF-8")
            reader.readLines().forEach {
                builder.append(it)
            }
            return builder.toString()
        } catch (e: IOException) {
            throw e
        }
    }
}
