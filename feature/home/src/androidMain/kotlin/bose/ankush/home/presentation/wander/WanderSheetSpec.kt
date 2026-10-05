package bose.ankush.home.presentation.wander

/**
 * How a [WanderSheetPanel] opens and closes.
 *
 * @param peekFraction share of the area long content opens at; null always opens fully.
 * @param heightFraction fixed card height as a share of the area; null wraps the content.
 * @param key a new key resets the drag position, like a new alert does.
 */
internal data class WanderSheetSpec(
    val onDismiss: () -> Unit,
    val closeLabel: String,
    val peekFraction: Float? = DEFAULT_PEEK_FRACTION,
    val heightFraction: Float? = null,
    val key: Any? = null,
)

private const val DEFAULT_PEEK_FRACTION = 0.55f
