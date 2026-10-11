package bose.ankush.home.presentation.screen

import kotlin.math.pow
import kotlin.math.roundToInt

internal const val VALUE_PLACEHOLDER = "--"

/**
 * Formats a numeric weather value for the home screen. Null or non-finite becomes [VALUE_PLACEHOLDER].
 */
internal fun formatAnimatedNumber(
    value: Double?,
    decimals: Int = 0,
    suffix: String = "",
    placeholder: String = VALUE_PLACEHOLDER,
): String {
    if (value == null || value.isNaN() || value.isInfinite()) return placeholder
    val text =
        if (decimals <= 0) {
            value.roundToInt().toString()
        } else {
            val factor = 10.0.pow(decimals)
            val scaled = (value * factor).roundToInt() / factor
            val raw = scaled.toString()
            val dot = raw.indexOf('.')
            when {
                dot < 0 -> raw + "." + "0".repeat(decimals)
                raw.length - dot - 1 < decimals -> raw + "0".repeat(decimals - (raw.length - dot - 1))
                else -> raw
            }
        }
    return text + suffix
}

/** True when [text] is only the placeholder (no countable number). */
internal fun isValuePlaceholder(text: String): Boolean {
    val trimmed = text.trim()
    return trimmed.isEmpty() || trimmed == VALUE_PLACEHOLDER || trimmed == "—"
}
