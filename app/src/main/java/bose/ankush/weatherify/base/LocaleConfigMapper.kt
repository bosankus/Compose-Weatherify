package bose.ankush.weatherify.base

import android.content.Context
import com.google.gson.GsonBuilder

object LocaleConfigMapper {
    fun getAvailableLanguagesFromJson(
        jsonFile: String,
        context: Context,
    ): Array<String> {
        val jsonString =
            context.assets
                .open(jsonFile)
                .bufferedReader()
                .use { it.readText() }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val map = gson.fromJson(jsonString, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val languages = map["languages"] as List<String>
        return languages.toTypedArray()
    }
}
