package bose.ankush.language.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

internal object LocaleHelper {

    fun String.getDisplayName(): String {
        val locale = Locale(this)
        return locale.getDisplayName(locale)
    }

    fun changeLanguageTo(languageCode: String): String {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
}