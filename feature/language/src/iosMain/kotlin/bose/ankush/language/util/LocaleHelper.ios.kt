package bose.ankush.language.util

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleIdentifier
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale

private const val PREFERRED_LANGUAGE_KEY = "bose.ankush.language.preferred"

internal actual fun platformGetDefaultLanguage(): String =
    NSUserDefaults.standardUserDefaults.stringForKey(PREFERRED_LANGUAGE_KEY)
        ?: (NSLocale.currentLocale.objectForKey(NSLocaleIdentifier) as? String)
        ?: "en"

internal actual fun platformChangeLanguageTo(languageCode: String): String {
    NSUserDefaults.standardUserDefaults.setObject(languageCode, forKey = PREFERRED_LANGUAGE_KEY)
    return languageCode
}

internal actual fun platformGetDisplayName(languageTag: String): String {
    val tag = languageTag.ifBlank { platformGetDefaultLanguage() }
    return NSLocale(localeIdentifier = tag).displayNameForKey(NSLocaleIdentifier, tag) ?: tag
}
