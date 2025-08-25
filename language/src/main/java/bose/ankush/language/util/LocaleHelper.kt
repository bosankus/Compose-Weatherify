package bose.ankush.language.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

internal object LocaleHelper {

    fun String.getCountryFlag(): String {
        val countryCode = this.split("-").lastOrNull()?.uppercase(Locale.getDefault()) ?: return ""

        if (countryCode.length != 2) return "\uD83C\uDF3F"

        val flagOffset = 0x1F1E6
        val asciiOffset = 0x41
        val firstChar = countryCode[0].code - asciiOffset + flagOffset
        val secondChar = countryCode[1].code - asciiOffset + flagOffset
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

    fun String.getDisplayName(): String {
        val languageCode = this.split("-").firstOrNull() ?: this
        val locale = Locale.forLanguageTag(languageCode)
        return locale.getDisplayName(locale)
    }

    fun changeLanguageTo(languageCode: String): String {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .ifEmpty { getDefaultLanguage() }
    }

    fun getDefaultLanguage(): String = Locale.getDefault().toLanguageTag()
}