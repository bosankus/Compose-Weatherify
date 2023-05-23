package bose.ankush.language.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

internal object LocaleHelper {

    fun String.getDisplayName(): String {
        val locale = Locale(this)
        return locale.displayName
    }

    fun changeLanguageTo(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }
}