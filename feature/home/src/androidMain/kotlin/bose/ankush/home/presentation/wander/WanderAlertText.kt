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

/** A body section as the panel shows it. A null [heading] means the body runs without a title. */
internal data class AlertDisplaySection(
    val heading: String?,
    val section: AlertSection,
)

/**
 * Panel order: What and Impacts first with no heading, then Where as "Impacted areas",
 * then every other section in its original order under its own label. When stays the summary.
 */
internal fun ParsedAlert.displaySections(): List<AlertDisplaySection> {
    val body = bodySections
    val leading = listOf(WHAT_LABEL, IMPACTS_LABEL)
    val ordered =
        leading.flatMap { label -> body.filter { it.label == label }.map { AlertDisplaySection(null, it) } } +
            body.filter { it.label == WHERE_LABEL }.map { AlertDisplaySection(IMPACTED_AREAS_HEADING, it) }
    val placed = leading + WHERE_LABEL
    return ordered + body.filterNot { it.label in placed }.map { AlertDisplaySection(it.label, it) }
}

internal fun parseAlertDescription(raw: String?): ParsedAlert {
    val text = raw?.replace("\r\n", "\n")?.replace('\r', '\n').orEmpty()
    val markers = SECTION_MARKER.findAll(text).toList()
    return when {
        text.isBlank() -> ParsedAlert()
        markers.isEmpty() -> ParsedAlert(fallback = cleanAlertBody(text))
        else -> {
            val sections = mutableListOf<AlertSection>()
            val preamble = cleanAlertBody(text.substring(0, markers.first().range.first))
            if (preamble.isNotEmpty()) sections += AlertSection(label = null, body = preamble)
            markers.forEachIndexed { index, match ->
                val end = markers.getOrNull(index + 1)?.range?.first ?: text.length
                val label = match.groupValues[1].toSectionLabel()
                val body = cleanAlertBody(text.substring(match.range.last + 1, end))
                if (body.isNotEmpty()) {
                    val items = if (label == WHERE_LABEL) splitAlertAreas(body) else null
                    sections += AlertSection(label = label, body = body, items = items)
                }
            }
            ParsedAlert(sections = sections)
        }
    }
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
    val items =
        body
            .takeUnless { it.contains('\n') }
            ?.removeSuffix(".")
            ?.split(',')
            ?.map {
                it
                    .trim()
                    .removePrefix("and ")
                    .removePrefix("And ")
                    .trim()
            }
    return items?.takeIf { it.size >= 2 && it.none(String::isEmpty) }
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
private const val WHAT_LABEL = "What"
private const val IMPACTS_LABEL = "Impacts"
internal const val IMPACTED_AREAS_HEADING = "Impacted areas"
