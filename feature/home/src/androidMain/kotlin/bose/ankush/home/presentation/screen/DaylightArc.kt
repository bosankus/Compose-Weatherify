package bose.ankush.home.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.time.Clock

/**
 * Static sunrise and sunset. The sun sits on the upper arc for the current time.
 * It does not move. The arc, hatch, and glow stay inside the canvas bounds.
 */
@Composable
internal fun DaylightArc(
    sunrise: Long?,
    sunset: Long?,
    contentColor: Color,
    modifier: Modifier = Modifier,
    timezoneOffset: Int? = null,
) {
    val zone = forecastZone(timezoneOffset)
    val progress = sunAlongDay(sunrise, sunset, Clock.System.now().epochSeconds)
    val sunriseLabel = sunrise?.toClock(zone) ?: UNAVAILABLE
    val sunsetLabel = sunset?.toClock(zone) ?: UNAVAILABLE
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Sunrise $sunriseLabel, sunset $sunsetLabel"
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SunLabel(
            time = sunriseLabel,
            caption = SUNRISE,
            contentColor = contentColor,
            align = TextAlign.Start,
        )
        DayArcCanvas(
            progress = progress,
            modifier =
                Modifier
                    .weight(1f)
                    .height(ArcHeight)
                    .padding(horizontal = 6.dp)
                    .clipToBounds(),
        )
        SunLabel(
            time = sunsetLabel,
            caption = SUNSET,
            contentColor = contentColor,
            align = TextAlign.Start,
        )
    }
}

/**
 * 0 at sunrise and earlier, 1 at sunset and later.
 * Missing or inverted times stay at the sunrise end.
 */
internal fun sunAlongDay(
    sunrise: Long?,
    sunset: Long?,
    nowEpochSeconds: Long,
): Float {
    val rise = sunrise
    val set = sunset
    return when {
        rise == null || set == null || set <= rise -> 0f
        nowEpochSeconds <= rise -> 0f
        nowEpochSeconds >= set -> 1f
        else -> {
            val span = (set - rise).toFloat()
            val elapsed = (nowEpochSeconds - rise).toFloat()
            (elapsed / span).coerceIn(0f, 1f)
        }
    }
}

@Composable
private fun SunLabel(
    time: String,
    caption: String,
    contentColor: Color,
    align: TextAlign,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = time,
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = align,
        )
        Text(
            text = caption,
            color = contentColor.copy(alpha = LABEL_ALPHA),
            fontSize = 12.sp,
            textAlign = align,
        )
    }
}

@Composable
private fun DayArcCanvas(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val glow = GlowRadius.toPx()
        val sunRadius = SunRadius.toPx()
        val bottom = size.height - glow
        val radius =
            min(
                size.width / 2f - glow,
                bottom - glow,
            ).coerceAtLeast(1f)
        val center = Offset(size.width / 2f, bottom)
        val oval =
            Rect(
                left = center.x - radius,
                top = center.y - radius,
                right = center.x + radius,
                bottom = center.y + radius,
            )
        val bowl =
            Path().apply {
                arcTo(oval, startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = true)
                close()
            }
        clipPath(bowl) {
            val spacing = HatchSpacing.toPx()
            val reach = size.width + size.height
            var x = -size.height
            while (x < reach) {
                drawLine(
                    color = Hatch,
                    start = Offset(x, 0f),
                    end = Offset(x + size.height, size.height),
                    strokeWidth = HatchStroke.toPx(),
                )
                x += spacing
            }
        }
        drawPath(
            path =
                Path().apply {
                    arcTo(oval, startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = true)
                },
            color = Arc,
            style =
                Stroke(
                    width = ArcStroke.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(Dash.toPx(), DashGap.toPx()),
                        ),
                ),
        )
        val theta = (PI * (1.0 - progress.toDouble())).toFloat()
        val sun =
            Offset(
                x = center.x + radius * cos(theta),
                y = center.y - radius * sin(theta),
            )
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(SunGlow, Color.Transparent),
                    center = sun,
                    radius = glow,
                ),
            radius = glow,
            center = sun,
        )
        drawCircle(color = Sun, radius = sunRadius, center = sun)
    }
}

private const val SUNRISE = "Sunrise"
private const val SUNSET = "Sunset"
private const val UNAVAILABLE = "N/A"
private const val LABEL_ALPHA = 0.62f

private val ArcHeight = 78.dp
private val GlowRadius = 12.dp
private val SunRadius = 5.dp
private val HatchSpacing = 5.dp
private val HatchStroke = 1.1.dp
private val ArcStroke = 1.4.dp
private val Dash = 1.6.dp
private val DashGap = 3.4.dp

private val Hatch = Color.Black.copy(alpha = 0.42f)
private val Arc = Color(0xFFD7D4CC)
private val Sun = Color(0xFFFFD24A)
private val SunGlow = Color(0xFFFFD24A).copy(alpha = 0.55f)
