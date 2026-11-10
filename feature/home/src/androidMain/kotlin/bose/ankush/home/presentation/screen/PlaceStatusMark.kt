package bose.ankush.home.presentation.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.theme.ToastOnWarning
import bose.ankush.commonui.theme.WarningYellow

/**
 * Right end of a place card, drawn as one mark that morphs between states: the remove X
 * twists down into a small spinning ring while Home fetches the picked place, and the ring
 * then fills into the yellow disc as the tick draws itself in.
 */
@Composable
internal fun PlaceStatusMark(
    trailing: PlaceCardTrailing,
    deleteLabel: String,
    contentColor: Color,
    onDelete: () -> Unit,
) {
    val transition = updateTransition(trailing, label = "placeStatusMark")
    val cross by transition.animateFloat({ tween(MORPH_MILLIS) }, label = "cross") {
        if (it == PlaceCardTrailing.Remove) 1f else 0f
    }
    val ring by transition.animateFloat({ tween(MORPH_MILLIS) }, label = "ring") {
        if (it == PlaceCardTrailing.Switching) 1f else 0f
    }
    val disc by transition.animateFloat(
        {
            if (targetState == PlaceCardTrailing.Selected) {
                spring(dampingRatio = DISC_DAMPING, stiffness = Spring.StiffnessMediumLow)
            } else {
                tween(MORPH_MILLIS)
            }
        },
        label = "disc",
    ) { if (it == PlaceCardTrailing.Selected) 1f else 0f }
    val tick by transition.animateFloat(
        {
            if (targetState == PlaceCardTrailing.Selected) {
                tween(TICK_MILLIS, delayMillis = TICK_DELAY_MILLIS)
            } else {
                tween(MORPH_MILLIS / 2)
            }
        },
        label = "tick",
    ) { if (it == PlaceCardTrailing.Selected) 1f else 0f }

    val spin = remember { Animatable(0f) }
    val switching = trailing == PlaceCardTrailing.Switching
    LaunchedEffect(switching) {
        while (switching) {
            spin.snapTo(spin.value % FULL_TURN)
            spin.animateTo(spin.value + FULL_TURN, tween(SPIN_MILLIS, easing = LinearEasing))
        }
    }

    val removable = trailing == PlaceCardTrailing.Remove
    // GPS keeps its narrow gap at rest; any other state takes the remove button's 48dp slot.
    val settledEmpty =
        transition.currentState == PlaceCardTrailing.None && trailing == PlaceCardTrailing.None
    Box(
        modifier =
            Modifier
                .animateContentSize(tween(MORPH_MILLIS))
                .then(if (settledEmpty) Modifier.width(8.dp) else Modifier.size(48.dp))
                .clip(CircleShape)
                .then(
                    if (removable) {
                        Modifier
                            .clickable(role = Role.Button, onClick = onDelete)
                            .semantics { contentDescription = deleteLabel }
                    } else {
                        Modifier
                    },
                ).then(
                    if (switching) {
                        Modifier.semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        }
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (settledEmpty) return@Box
        Canvas(modifier = Modifier.size(28.dp)) {
            if (disc > 0f) drawCircle(WarningYellow, radius = size.minDimension / 2 * disc)
            if (cross > 0f) drawCross(contentColor.copy(alpha = MUTED_ALPHA), cross)
            if (ring > 0f) drawRing(WarningYellow, ring, spin.value)
            if (tick > 0f) drawTick(ToastOnWarning, tick)
        }
    }
}

/** The X: arms shrink to the centre and twist a quarter turn as [progress] falls to 0. */
private fun DrawScope.drawCross(
    color: Color,
    progress: Float,
) {
    val arm = 5.dp.toPx() * progress
    val stroke = 2.dp.toPx()
    rotate(degrees = (1f - progress) * QUARTER_TURN) {
        drawLine(
            color,
            center - Offset(arm, arm),
            center + Offset(arm, arm),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            center + Offset(-arm, arm),
            center + Offset(arm, -arm),
            stroke,
            StrokeCap.Round
        )
    }
}

/** A three-quarter ring that grows out of nothing as [progress] rises, turned by [angle]. */
private fun DrawScope.drawRing(
    color: Color,
    progress: Float,
    angle: Float,
) {
    val radius = 8.dp.toPx()
    drawArc(
        color = color,
        startAngle = angle - QUARTER_TURN,
        sweepAngle = RING_SWEEP * progress,
        useCenter = false,
        topLeft = center - Offset(radius, radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )
}

/** The tick, drawn from its short stroke to its long one as [progress] rises. */
private fun DrawScope.drawTick(
    color: Color,
    progress: Float,
) {
    val unit = 1.dp.toPx()
    val full =
        Path().apply {
            moveTo(center.x - 4.5f * unit, center.y + 0.2f * unit)
            lineTo(center.x - 1.4f * unit, center.y + 3.2f * unit)
            lineTo(center.x + 4.6f * unit, center.y - 3.4f * unit)
        }
    val measure = PathMeasure().apply { setPath(full, false) }
    val drawn = Path()
    measure.getSegment(0f, measure.length * progress, drawn, true)
    drawPath(
        drawn,
        color,
        style = Stroke(width = 2.2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private const val MORPH_MILLIS = 220
private const val TICK_MILLIS = 260
private const val TICK_DELAY_MILLIS = 90
private const val SPIN_MILLIS = 800
private const val DISC_DAMPING = 0.6f
private const val FULL_TURN = 360f
private const val QUARTER_TURN = 90f
private const val RING_SWEEP = 270f
private const val MUTED_ALPHA = 0.72f
