package bose.ankush.weatherify.wear.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import bose.ankush.weatherify.wear.presentation.HourlyUiState
import bose.ankush.weatherify.wear.presentation.components.isSmallWatch
import bose.ankush.weatherify.wear.presentation.components.polar
import bose.ankush.weatherify.wear.presentation.components.toImageVector
import bose.ankush.weatherify.wear.presentation.theme.DialColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Hours sit on two arcs, six down the right and six up the left, leaving 12 o'clock
// free for the TimeText and 6 o'clock free for the page indicator.
private const val SLOT_DEGREES = 24f
private const val SLOTS_PER_SIDE = 6
private const val RIGHT_ARC_START = -60f
private const val LEFT_ARC_START = 120f

private fun slotAngle(index: Int): Float =
    if (index < SLOTS_PER_SIDE) {
        RIGHT_ARC_START + index * SLOT_DEGREES
    } else {
        LEFT_ARC_START + (index - SLOTS_PER_SIDE) * SLOT_DEGREES
    }
private const val RING_RADIUS_FRACTION = 0.6f
private const val LABEL_RADIUS_FRACTION = 0.84f

// Crown travel needed to move one hour; low enough that a flick crosses several hours.
private const val ROTARY_PIXELS_PER_STEP = 40f

/**
 * The next twelve hours around the dial, clockwise from 1 o'clock. Turn the crown (or tap
 * an hour) to pick one; the centre shows that hour's forecast and the red arc, drawn from
 * 12 o'clock, tracks how far ahead it is.
 */
@Composable
internal fun HourlyDialScreen(hours: List<HourlyUiState>) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    val focusRequester = rememberActiveFocusRequester()
    val small = isSmallWatch()

    val selectedAngle by animateFloatAsState(
        targetValue = slotAngle(selected),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "hourly-arc",
    )
    val hour = hours[selected.coerceIn(hours.indices)]

    ScreenScaffold {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onRotaryScrollEvent { event ->
                        rotaryAccumulator += event.verticalScrollPixels
                        val steps = (rotaryAccumulator / ROTARY_PIXELS_PER_STEP).toInt()
                        if (steps != 0) {
                            rotaryAccumulator -= steps * ROTARY_PIXELS_PER_STEP
                            selected = (selected + steps).coerceIn(hours.indices)
                        }
                        true
                    }.focusRequester(focusRequester)
                    .focusable(),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = min(size.width, size.height) / 2f * RING_RADIUS_FRACTION
                val stroke = 1.5.dp.toPx()
                drawCircle(
                    color = DialColors.Crimson.copy(alpha = 0.75f),
                    radius = radius,
                    style =
                        Stroke(
                            width = stroke,
                            pathEffect =
                                PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
                        ),
                )
                // Sweep is measured from 12 o'clock (-90°) to the selected hour.
                val sweep = selectedAngle + 90f
                if (sweep > 0.01f) {
                    drawArc(
                        brush = DialColors.AccentGradient,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                val angle = Math.toRadians(selectedAngle.toDouble())
                drawCircle(
                    color = DialColors.Crimson,
                    radius = 4.dp.toPx(),
                    center =
                        Offset(
                            center.x + radius * cos(angle).toFloat(),
                            center.y + radius * sin(angle).toFloat(),
                        ),
                )
            }

            hours.forEachIndexed { index, item ->
                val isSelected = index == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .polar(slotAngle(index), LABEL_RADIUS_FRACTION)
                            .size(if (small) 26.dp else 30.dp)
                            .clip(CircleShape)
                            .clickable { selected = index }
                            .semantics { contentDescription = "${item.time}, ${item.temperature}" },
                ) {
                    Text(
                        text = item.hourLabel,
                        fontSize = if (isSelected) 15.sp else if (small) 11.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) DialColors.TextPrimary else DialColors.TextMuted,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.polar(-90f, 0.1f),
            ) {
                Text(
                    text = hour.temperature,
                    fontSize = if (small) 30.sp else 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = DialColors.TextPrimary,
                )
                Text(
                    text = hour.time,
                    fontSize = if (small) 12.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DialColors.TextMuted,
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .polar(90f, 0.38f)
                        .size(if (small) 30.dp else 36.dp)
                        .clip(CircleShape)
                        .background(DialColors.AccentGradient)
                        .semantics { contentDescription = hour.condition ?: "" },
            ) {
                Icon(
                    imageVector = hour.icon.toImageVector(),
                    contentDescription = null,
                    tint = DialColors.TextPrimary,
                    modifier = Modifier.size(if (small) 16.dp else 18.dp),
                )
            }
        }
    }
}
