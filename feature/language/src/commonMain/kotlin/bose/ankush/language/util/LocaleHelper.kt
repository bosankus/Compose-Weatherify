package bose.ankush.language.util

internal expect fun platformGetDefaultLanguage(): String

internal expect fun platformChangeLanguageTo(languageCode: String): String

internal expect fun platformGetDisplayName(languageTag: String): String

internal object LocaleHelper {
    fun String.getDisplayName(): String = platformGetDisplayName(this)

    fun changeLanguageTo(languageCode: String): String = platformChangeLanguageTo(languageCode)

    fun getDefaultLanguage(): String = platformGetDefaultLanguage()
}
