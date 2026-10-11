package bose.ankush.language.util

/**
 * Which of [languages] is the app's current [locale]. The platform reports the device locale
 * (say `en-US`) while the list holds regional tags (`en-IN`), and Android swaps legacy and
 * modern codes (`iw` / `he`), so: an exact tag first, then the same language, else null.
 */
internal fun matchLanguage(
    languages: List<String>,
    locale: String,
): String? {
    val wanted = locale.normalizedTag()
    if (wanted.isEmpty()) return null
    return languages.firstOrNull { it.normalizedTag() == wanted }
        ?: languages.firstOrNull { it.languageCode() == wanted.languageCode() }
}

private fun String.normalizedTag(): String {
    val parts =
        trim()
            .replace('_', '-')
            .lowercase()
            .split('-')
            .filter { it.isNotEmpty() }
    if (parts.isEmpty()) return ""
    return (listOf(LEGACY_CODES[parts[0]] ?: parts[0]) + parts.drop(1)).joinToString("-")
}

private fun String.languageCode(): String = normalizedTag().substringBefore('-')

/** Codes Java and Android still emit for these languages. */
private val LEGACY_CODES = mapOf("iw" to "he", "in" to "id", "ji" to "yi")
