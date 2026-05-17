package bose.ankush.language.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

private const val REGIONAL_INDICATOR_SYMBOL_LETTER_A = 0x1F1E6
private const val ASCII_UPPERCASE_A = 0x41

internal object LocaleHelper {
    fun String.getCountryFlag(): String {
        val countryCode =
            split("-").lastOrNull()?.uppercase(Locale.getDefault())
                ?: return ""
        return if (countryCode.length == 2) {
            val firstChar =
                countryCode[0].code - ASCII_UPPERCASE_A + REGIONAL_INDICATOR_SYMBOL_LETTER_A
            val secondChar =
                countryCode[1].code - ASCII_UPPERCASE_A + REGIONAL_INDICATOR_SYMBOL_LETTER_A
            String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        } else {
            "🌿"
        }
    }

    fun String.getDisplayName(): String {
        val locale =
            if (this.isBlank()) {
                Locale.getDefault()
            } else {
                Locale.forLanguageTag(this)
            }
        return locale.getDisplayName(locale)
    }

    fun changeLanguageTo(languageCode: String): String {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        return AppCompatDelegate
            .getApplicationLocales()
            .toLanguageTags()
            .ifEmpty { getDefaultLanguage() }
    }

    fun getDefaultLanguage(): String = Locale.getDefault().toLanguageTag()
}
