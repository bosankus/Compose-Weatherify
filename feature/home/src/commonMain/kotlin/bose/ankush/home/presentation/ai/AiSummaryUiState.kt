package bose.ankush.home.presentation.ai

/** Where the summary sheet is in its life. The data layer only has to move this forward. */
enum class AiSummaryPhase {
    /** Nothing to show yet: the sheet draws placeholder lines. */
    Loading,

    /** [AiSummaryUiState.text] holds the summary, whole or still arriving. */
    Ready,

    /** Generation failed. The sheet offers a retry. */
    Failed,
}

/** Which glyph a tip chip wears. The wording itself comes from the rules, in [AiSummaryTip.label]. */
enum class AiSummaryTipKind { Umbrella, Sunscreen, Wind, Jacket }

data class AiSummaryTip(
    val kind: AiSummaryTipKind,
    val label: String,
)

/**
 * Everything the weather summary sheet draws. Both platforms read this one shape.
 *
 * @param text the summary so far; gr   ows while [isStreaming] is true.
 * @param tips rule-based chips shown under the text (umbrella, sunscreen, ...).
 * @param place the place the summary is for, shown under the title when set.
 */
data class AiSummaryUiState(
    val phase: AiSummaryPhase = AiSummaryPhase.Loading,
    val text: String = "",
    val isStreaming: Boolean = false,
    val tips: List<AiSummaryTip> = emptyList(),
    val place: String? = null,
)
