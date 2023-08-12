package bose.ankush.weatherify.data.room

import com.google.gson.Gson
import java.lang.reflect.Type

class JsonParser (private val gson: Gson) : Parser {
    override fun <T> fromJson(json: String, type: Type): T? {
        return gson.fromJson(json, type)
    }

    override fun <T> toJson(obj: T, type: Type): String? {
        return gson.toJson(obj, type)
    }
}

interface Parser {
    fun <T> fromJson(json: String, type: Type): T?
    fun <T> toJson(obj: T, type: Type): String?
}