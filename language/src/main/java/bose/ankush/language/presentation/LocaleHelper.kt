package bose.ankush.language.presentation

import java.util.Locale

object LocaleHelper {

    fun String.getDisplayName(): String {
        val locale = Locale(this)
        return locale.displayName
    }

    /*fun changeLanguageTo(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }*/
}