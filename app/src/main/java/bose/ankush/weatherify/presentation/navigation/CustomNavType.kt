package bose.ankush.weatherify.presentation.navigation

import android.os.Bundle
import androidx.navigation.NavType

class StringListType : NavType<List<String>>(isNullableAllowed = false) {
    override fun get(
        bundle: Bundle,
        key: String,
    ): List<String> {
        val stringArray = bundle.getStringArray(key)
        return stringArray?.toList() ?: emptyList()
    }

    override fun parseValue(value: String): List<String> = value.split(",").map { it.trim() }

    override fun put(
        bundle: Bundle,
        key: String,
        value: List<String>,
    ) {
        bundle.putStringArray(key, value.toTypedArray())
    }
}
