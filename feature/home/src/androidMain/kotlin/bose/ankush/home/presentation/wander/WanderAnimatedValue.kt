package bose.ankush.home.presentation.wander

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import kotlin.math.pow
import kotlin.math.roundToInt

internal const val WANDER_PLACEHOLDER = "--"

/**
 * Formats a numeric weather value for Wander. Null or non-finite becomes [WANDER_PLACEHOLDER].
 */
internal fun formatWanderNumber(
    value: Double?,
    decimals: Int = 0,
    suffix: String = "",
    placeholder: String = WANDER_PLACEHOLDER,
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
internal fun isWanderPlaceholder(text: String): Boolean {
    val trimmed = text.trim()
    return trimmed.isEmpty() || trimmed == WANDER_PLACEHOLDER || trimmed == "—"
}

/**
 * Pulls the leading number from a formatted Wander value such as "21°", "3.5 m/s", or "40%".
 */
internal fun parseWanderNumber(text: String): Double? =
    NUMBER_IN_TEXT
        .find(text.trim())
        ?.takeUnless { isWanderPlaceholder(text) }
        ?.value
        ?.toDoubleOrNull()

/**
 * Keeps the unit/suffix from [template] while swapping in [numberText].
 * Example: template "21°", numberText "22" → "22°".
 */
internal fun applyWanderNumber(
    template: String,
    numberText: String,
): String {
    val match = NUMBER_IN_TEXT.find(template)
    return if (match == null ||
        isWanderPlaceholder(template)
    ) {
        numberText
    } else {
        template.replaceRange(match.range, numberText)
    }
}

private fun decimalPlaces(text: String): Int {
    val match = NUMBER_IN_TEXT.find(text.trim()) ?: return 0
    val body = match.value
    val dot = body.indexOf('.')
    return if (dot < 0) 0 else body.length - dot - 1
}

private fun formatNumberBody(
    value: Double,
    decimals: Int,
): String = formatWanderNumber(value, decimals = decimals, suffix = "")

/**
 * Numeric values count up quickly; non-numeric text crossfades. Tabular figures keep width steady.
 */
@Suppress("LongParameterList")
@Composable
internal fun WanderAnimatedValue(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    style: TextStyle? = null,
    maxLines: Int = 1,
) {
    val targetNumber = parseWanderNumber(text)
    var displayed by remember { mutableStateOf(text) }
    val anim = remember { Animatable(0f) }

    LaunchedEffect(text) {
        val to = parseWanderNumber(text)
        if (to == null) {
            displayed = text
            return@LaunchedEffect
        }
        val from =
            parseWanderNumber(displayed)
                ?: if (isWanderPlaceholder(displayed)) 0.0 else to
        val decimals = decimalPlaces(text)
        anim.snapTo(from.toFloat())
        anim.animateTo(to.toFloat(), animationSpec = tween(durationMillis = VALUE_ANIM_MS)) {
            displayed = applyWanderNumber(text, formatNumberBody(value.toDouble(), decimals))
        }
        displayed = text
    }

    val baseStyle =
        (style ?: TextStyle.Default).copy(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFeatureSettings = "tnum",
        )

    if (targetNumber == null) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                fadeIn(tween(TEXT_CROSSFADE_MS)) togetherWith fadeOut(tween(TEXT_CROSSFADE_MS))
            },
            label = "wanderTextCrossfade",
            modifier = modifier,
        ) { value ->
            Text(text = value, style = baseStyle, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Text(
            text = displayed,
            style = baseStyle,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

private val NUMBER_IN_TEXT = Regex("""-?\d+(?:\.\d+)?""")
private const val VALUE_ANIM_MS = 500
private const val TEXT_CROSSFADE_MS = 220
