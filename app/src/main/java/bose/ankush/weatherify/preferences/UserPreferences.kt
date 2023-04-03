package bose.ankush.weatherify.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(val context: Context) : ManagedPreferences() {

    override fun getSharedPreferences(): SharedPreferences =
        context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE)

    fun getLocaleString(): String? = get(LOCALE, "")

    fun setLocaleString(localeString: String) {
        put(LOCALE, localeString)
    }

    fun setSelectedAppLanguageName(localeString: String) {
        put(SELECTED_APP_LANGUAGE_NAME, localeString)
    }

    companion object {
        const val SELECTED_APP_LANGUAGE_NAME = "selectedAppLanguageName"
        const val APP_SETTINGS = "app_settings"
        const val LOCALE = "locale"
    }
}