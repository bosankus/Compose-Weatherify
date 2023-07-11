package bose.ankush.weatherify.base

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object LocaleConfigMapper {

    fun getAvailableLanguagesFromJson(jsonFile: String, context: Context): Array<String> {
        // read JSON file
        val jsonString: String = context.assets.open(jsonFile)
            .bufferedReader()
            .use { it.readText() }

        // convert JSON to Map
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val map = gson.fromJson(jsonString, Map::class.java)


        // extract language data from JSON and return as Array
        val languages = map["languages"] as List<String>
        return languages.toTypedArray()
    }
}
