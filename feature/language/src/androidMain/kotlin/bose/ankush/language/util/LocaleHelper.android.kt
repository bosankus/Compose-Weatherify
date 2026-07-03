package bose.ankush.language.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

internal actual fun platformGetDefaultLanguage(): String = Locale.getDefault().toLanguageTag()

internal actual fun platformChangeLanguageTo(languageCode: String): String {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
    return AppCompatDelegate
        .getApplicationLocales()
        .toLanguageTags()
        .ifEmpty { platformGetDefaultLanguage() }
}

internal actual fun platformGetDisplayName(languageTag: String): String {
    val locale = if (languageTag.isBlank()) Locale.getDefault() else Locale.forLanguageTag(languageTag)
    return locale.getDisplayName(locale)
}
