package bose.ankush.language.util

private const val REGIONAL_INDICATOR_SYMBOL_LETTER_A = 0x1F1E6
private const val ASCII_UPPERCASE_A = 0x41
private const val SUPPLEMENTARY_PLANE_START = 0x10000
private const val HIGH_SURROGATE_START = 0xD800
private const val LOW_SURROGATE_START = 0xDC00
private const val SURROGATE_SHIFT = 10
private const val LOW_SURROGATE_MASK = 0x3FF

internal expect fun platformGetDefaultLanguage(): String

internal expect fun platformChangeLanguageTo(languageCode: String): String

internal expect fun platformGetDisplayName(languageTag: String): String

internal object LocaleHelper {
    fun String.getCountryFlag(): String {
        val countryCode = split("-").lastOrNull()?.uppercase() ?: return ""
        return if (countryCode.length == 2) {
            val firstChar =
                countryCode[0].code - ASCII_UPPERCASE_A + REGIONAL_INDICATOR_SYMBOL_LETTER_A
            val secondChar =
                countryCode[1].code - ASCII_UPPERCASE_A + REGIONAL_INDICATOR_SYMBOL_LETTER_A
            codePointToString(firstChar) + codePointToString(secondChar)
        } else {
            "🌿"
        }
    }

    fun String.getDisplayName(): String = platformGetDisplayName(this)

    fun changeLanguageTo(languageCode: String): String = platformChangeLanguageTo(languageCode)

    fun getDefaultLanguage(): String = platformGetDefaultLanguage()
}

/** UTF-16 surrogate-pair encoding for supplementary-plane code points (e.g. regional indicators). */
private fun codePointToString(codePoint: Int): String {
    if (codePoint < SUPPLEMENTARY_PLANE_START) return codePoint.toChar().toString()
    val offset = codePoint - SUPPLEMENTARY_PLANE_START
    val high = (offset shr SURROGATE_SHIFT) + HIGH_SURROGATE_START
    val low = (offset and LOW_SURROGATE_MASK) + LOW_SURROGATE_START
    return charArrayOf(high.toChar(), low.toChar()).concatToString()
}
