package bose.ankush.home.presentation.wander

/**
 * An alert description split for display. [sections] come from NWS style `* LABEL...`
 * markers, in order. [fallback] is the whole cleaned text when there are no markers.
 * Both are empty when the description is blank. Text is only re-wrapped, never rewritten.
 */
internal data class ParsedAlert(
    val sections: List<AlertSection> = emptyList(),
    val fallback: String? = null,
) {
    /** The When body, shown as the one-line summary under the title. */
    val summary: String?
        get() = sections.firstOrNull { it.label == WHEN_LABEL }?.body

    /** Sections to list in the body. When is left out once it is the summary. */
    val bodySections: List<AlertSection>
        get() = if (summary != null) sections.filterNot { it.label == WHEN_LABEL } else sections
}

/**
 * One section. [label] is sentence case (What, Where, Additional details), or null for text
 * before the first marker. [items] is the Where area list when a comma split is safe.
 */
internal data class AlertSection(
    val label: String?,
    val body: String,
    val items: List<String>? = null,
)

internal fun parseAlertDescription(raw: String?): ParsedAlert {
    val text = raw?.replace("\r\n", "\n")?.replace('\r', '\n').orEmpty()
    if (text.isBlank()) return ParsedAlert()
    val markers = SECTION_MARKER.findAll(text).toList()
    if (markers.isEmpty()) return ParsedAlert(fallback = cleanAlertBody(text))
    val sections = mutableListOf<AlertSection>()
    val preamble = cleanAlertBody(text.substring(0, markers.first().range.first))
    if (preamble.isNotEmpty()) sections += AlertSection(label = null, body = preamble)
    markers.forEachIndexed { index, match ->
        val end = markers.getOrNull(index + 1)?.range?.first ?: text.length
        val label = match.groupValues[1].toSectionLabel()
        val body = cleanAlertBody(text.substring(match.range.last + 1, end))
        if (body.isEmpty()) return@forEachIndexed
        val items = if (label == WHERE_LABEL) splitAlertAreas(body) else null
        sections += AlertSection(label = label, body = body, items = items)
    }
    return ParsedAlert(sections = sections)
}

/**
 * Hard-wrapped lines become one line. Blank lines stay as paragraph breaks.
 * Runs of whitespace collapse to one space.
 */
internal fun cleanAlertBody(raw: String): String =
    raw
        .split(PARAGRAPH_BREAK)
        .map { it.replace(WHITESPACE, " ").trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n\n")

/**
 * Comma split for the Where list. A leading "and " and the final period go. Returns null when
 * the split is not safe (one paragraph only, at least two non-empty areas).
 */
internal fun splitAlertAreas(body: String): List<String>? {
    if (body.contains('\n')) return null
    val items =
        body
            .removeSuffix(".")
            .split(',')
            .map {
                it
                    .trim()
                    .removePrefix("and ")
                    .removePrefix("And ")
                    .trim()
            }
    if (items.size < 2 || items.any { it.isEmpty() }) return null
    return items
}

private fun String.toSectionLabel(): String =
    replace(WHITESPACE, " ")
        .trim()
        .lowercase()
        .replaceFirstChar { it.uppercaseChar() }

private val SECTION_MARKER = Regex("""(?m)^[ \t]*\*[ \t]*([A-Za-z][A-Za-z /&'-]*?)[ \t]*\.\.\.""")
private val PARAGRAPH_BREAK = Regex("""\n[ \t]*\n""")
private val WHITESPACE = Regex("""\s+""")
private const val WHEN_LABEL = "When"
private const val WHERE_LABEL = "Where"
