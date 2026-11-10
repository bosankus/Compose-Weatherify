package bose.ankush.weatherify.wear.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import bose.ankush.weatherify.wear.presentation.WeatherUiState
import bose.ankush.weatherify.wear.presentation.components.isSmallWatch
import bose.ankush.weatherify.wear.presentation.components.polar
import bose.ankush.weatherify.wear.presentation.theme.DialColors
import kotlin.math.min

private data class DialReading(
    val value: String,
    val label: String,
)

// The readings fan across the upper 240° of the dial, from 8 o'clock round to 4 o'clock;
// the remaining lower wedge is left quiet for the location / alert line.
private const val FAN_START_DEGREES = 150f
private const val FAN_SWEEP_DEGREES = 240f
private const val HUB_RADIUS_FRACTION = 0.31f
private const val LABEL_RADIUS_FRACTION = 0.62f

// The wedge under 12 o'clock pulls its reading inward so it clears the TimeText overlay.
private const val TOP_LABEL_RADIUS_FRACTION = 0.53f

/**
 * The glance: current conditions as a segmented dial. Each wedge holds one reading, the
 * hub holds the temperature, and the quiet bottom wedge names the place (or the active
 * alert, which opens its details on tap).
 */
@Composable
internal fun DialScreen(
    uiState: WeatherUiState,
    onAlertClick: () -> Unit,
) {
    val readings =
        listOf(
            DialReading(uiState.feelsLike ?: "--", "FEELS"),
            DialReading(uiState.humidity ?: "--", "HUMIDITY"),
            DialReading(
                listOfNotNull(uiState.highTemp, uiState.lowTemp).joinToString("/").ifEmpty { "--" },
                "HIGH · LOW",
            ),
            DialReading(uiState.wind ?: "--", "WIND M/S"),
            DialReading(uiState.uvIndex ?: "--", uiState.uvLevel?.let { "UV · $it" } ?: "UV"),
        )
    val wedgeSweep = FAN_SWEEP_DEGREES / readings.size
    val small = isSmallWatch()

    // Wedges sweep open once when the dial first appears.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }

    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = min(size.width, size.height) / 2f
                val topLeft = Offset(center.x - radius, center.y - radius)
                val dialSize = Size(radius * 2, radius * 2)
                drawCircle(color = DialColors.DialBase, radius = radius)
                readings.indices.forEach { index ->
                    drawArc(
                        color = DialColors.Segments[index % DialColors.Segments.size],
                        startAngle = FAN_START_DEGREES + index * wedgeSweep,
                        // A hair of overlap hides anti-aliasing seams between wedges.
                        sweepAngle = (wedgeSweep + 0.5f) * reveal.value,
                        useCenter = true,
                        topLeft = topLeft,
                        size = dialSize,
                    )
                }
                // A darker rim, like a bezel, keeps the brightest wedge from touching the edge.
                val rim = radius * 0.06f
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = radius - rim / 2f,
                    style = Stroke(width = rim),
                )
                drawCircle(color = DialColors.Hub, radius = radius * HUB_RADIUS_FRACTION)
            }

            readings.forEachIndexed { index, reading ->
                val midAngle = FAN_START_DEGREES + (index + 0.5f) * wedgeSweep
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier.polar(
                            angleDegrees = midAngle,
                            radiusFraction =
                                if (midAngle in 240f..300f) TOP_LABEL_RADIUS_FRACTION else LABEL_RADIUS_FRACTION,
                        ),
                ) {
                    Text(
                        text = reading.value,
                        fontSize = if (small) 15.sp else 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DialColors.TextPrimary.copy(alpha = reveal.value),
                    )
                    Text(
                        text = reading.label,
                        fontSize = if (small) 8.sp else 9.sp,
                        letterSpacing = 0.5.sp,
                        color = DialColors.TextPrimary.copy(alpha = 0.72f * reveal.value),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = uiState.temperature,
                    fontSize = if (small) 20.sp else 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = DialColors.TextPrimary,
                )
                uiState.conditionShort?.let {
                    Text(
                        text = it,
                        fontSize = if (small) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        color = DialColors.Crimson,
                    )
                }
            }

            BottomLine(
                uiState = uiState,
                onAlertClick = onAlertClick,
                modifier = Modifier.polar(90f, 0.6f),
            )
        }
    }
}

@Composable
private fun BottomLine(
    uiState: WeatherUiState,
    onAlertClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alert = uiState.alert
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .then(if (alert != null) Modifier.clickable(onClick = onAlertClick) else Modifier)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = alert?.event ?: uiState.location,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            color = DialColors.TextPrimary.copy(alpha = 0.9f),
            // Long names drift instead of truncating; widthIn keeps them inside the chord.
            modifier =
                Modifier
                    .widthIn(max = 120.dp)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 800,
                        spacing = MarqueeSpacing(24.dp),
                    ),
        )
        Spacer(Modifier.height(4.dp))
        Icon(
            imageVector = if (alert != null) Icons.Filled.WarningAmber else Icons.Filled.Place,
            contentDescription = if (alert != null) "Weather alert, tap for details" else null,
            tint = DialColors.Crimson,
            modifier = Modifier.size(16.dp),
        )
    }
}
