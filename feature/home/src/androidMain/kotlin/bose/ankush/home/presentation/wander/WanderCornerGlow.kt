package bose.ankush.home.presentation.wander

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.max

/**
 * Soft top-right radial fade behind Wander content. Tint comes from the photo sample.
 * Drawn with [drawBehind] so it never takes pointer input. Alpha stays low because
 * [WanderConditionBackground] already lays a top readability scrim (~0x55 at the top).
 */
@Composable
internal fun WanderCornerGlow(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = CORNER_GLOW_ANIM_MS),
        label = "wanderCornerGlow",
    )
    if (animated.alpha <= 0.001f) return
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawBehind {
                    val radius = max(size.width, size.height) * CORNER_GLOW_RADIUS_FRACTION
                    val brush =
                        Brush.radialGradient(
                            colorStops =
                                arrayOf(
                                    0f to animated,
                                    CORNER_GLOW_MID_STOP to animated.copy(alpha = animated.alpha * 0.35f),
                                    1f to animated.copy(alpha = 0f),
                                ),
                            center = Offset(size.width, 0f),
                            radius = radius,
                        )
                    drawRect(brush = brush)
                },
    )
}

/**
 * Corner tint from the photo average and header luminance. Bright tops get a slightly
 * stronger soft dark; dark tops stay near invisible so the existing scrim is enough.
 */
internal fun wanderCornerGlowColor(
    averageColor: Color,
    headerLuminance: Double,
): Color {
    val brightShare =
        ((headerLuminance - HEADER_DIM_FLOOR) / (HEADER_BRIGHT_CEILING - HEADER_DIM_FLOOR))
            .toFloat()
            .coerceIn(0f, 1f)
    val alpha = CORNER_GLOW_ALPHA_MIN + (CORNER_GLOW_ALPHA_MAX - CORNER_GLOW_ALPHA_MIN) * brightShare
    val tint = darken(averageColor.copy(alpha = 1f), CORNER_GLOW_DARKEN)
    return tint.copy(alpha = alpha)
}

/** Matches the top scrim's ink so the two stack cleanly. */
private const val CORNER_GLOW_DARKEN = 0.42f

/** Softened vs the 0.18–0.28 solo range: KMM's top scrim already darkens the top (~0x55). */
private const val CORNER_GLOW_ALPHA_MIN = 0.08f
private const val CORNER_GLOW_ALPHA_MAX = 0.16f
private const val HEADER_DIM_FLOOR = 0.30
private const val HEADER_BRIGHT_CEILING = 0.80
private const val CORNER_GLOW_RADIUS_FRACTION = 0.50f
private const val CORNER_GLOW_MID_STOP = 0.45f
private const val CORNER_GLOW_ANIM_MS = 150
