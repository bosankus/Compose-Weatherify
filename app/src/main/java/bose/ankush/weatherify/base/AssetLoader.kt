package bose.ankush.weatherify.base

import android.content.Context
import com.google.gson.Gson
import java.io.IOException

class AssetLoader(val context: Context) {
    @Throws(IOException::class)
    inline fun <reified T> loadJSONAndConvertToObject(fileName: String): T {
        return context.assets.open(fileName).use { inputStream ->
            Gson().fromJson(inputStream.bufferedReader().use { it.readText() }, T::class.java)
        }
    }
}