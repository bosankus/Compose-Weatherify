package bose.ankush.weatherify.wear.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.tooling.preview.devices.WearDevices
import bose.ankush.weatherify.wear.data.WeatherUiMapper

private val AlertAmber = Color(0xFFFFD54F)

/** Watch faces span ~192dp to ~240dp; margins are a fraction of width so the layout
 * breathes the same on every size, with extra margin on round screens whose corners clip. */
@Composable
private fun responsiveHorizontalMargin(): androidx.compose.ui.unit.Dp {
    val configuration = LocalConfiguration.current
    val fraction = if (configuration.isScreenRound) 0.08f else 0.04f
    return (configuration.screenWidthDp * fraction).dp
}

/** Shown until [bose.ankush.weatherify.wear.data.WeatherSyncStore] has received a forecast
 * from the paired phone — there is no on-watch fetch path, so there's nothing to retry here. */
@Composable
internal fun WaitingForSyncScreen() {
    ScreenScaffold(
        modifier = Modifier.background(
            brush = WeatherIconType.UNKNOWN.toBackgroundGradient(DayPhase.NIGHT),
        ),
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "waiting-pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "waiting-pulse-alpha",
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = responsiveHorizontalMargin()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.WbCloudy,
                contentDescription = null,
                tint = WeatherIconType.UNKNOWN.toTextColor(DayPhase.NIGHT),
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer { alpha = pulseAlpha },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Waiting for weather from your phone…",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = WeatherIconType.UNKNOWN.toTextColor(DayPhase.NIGHT),
            )
        }
    }
}

@Composable
internal fun WeatherScreen(
    uiState: WeatherUiState,
    onAlertClick: () -> Unit,
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val textColor = uiState.icon.toTextColor(uiState.dayPhase)
    val horizontalMargin = responsiveHorizontalMargin()

    val content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit =
        { contentPadding ->
            TransformingLazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                state = columnState,
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalMargin),
            ) {
                // CurrentConditions is the entire glance: location, hero temp, and the
                // feels-like/humidity/wind line all live in one block so nothing needs
                // scrolling to be seen. Hourly is the only thing below the fold.
                item {
                    CurrentConditions(
                        uiState = uiState,
                        textColor = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    )
                }
                if (uiState.hourly.isNotEmpty()) {
                    item {
                        HourlyStrip(
                            hours = uiState.hourly,
                            textColor = textColor,
                            // requiredWidth escapes the column's horizontal margin so the
                            // strip runs edge to edge and hours slide in from the true
                            // screen border, not from inside the margin.
                            modifier = Modifier
                                .requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
                                .transformedHeight(this, transformationSpec)
                                .padding(top = 4.dp),
                        )
                    }
                }
            }
        }

    val background =
        Modifier.background(brush = uiState.icon.toBackgroundGradient(uiState.dayPhase))
    // The edgeButton slot is a separate non-nullable overload, hence the branch.
    val alert = uiState.alert
    if (alert != null) {
        ScreenScaffold(
            scrollState = columnState,
            modifier = background,
            edgeButton = { AlertEdgeButton(alert = alert, onClick = onAlertClick) },
        ) { contentPadding -> content(contentPadding) }
    } else {
        ScreenScaffold(
            scrollState = columnState,
            modifier = background,
        ) { contentPadding -> content(contentPadding) }
    }
}

/** Long names don't wrap or truncate — they drift slowly right-to-left instead.
 * initialDelayMillis must be set explicitly: in Immediately mode it defaults to
 * repeatDelayMillis, which leaves the name sitting clipped before the first scroll. */
private val marqueeModifier = Modifier.basicMarquee(
    iterations = Int.MAX_VALUE,
    repeatDelayMillis = 1200,
    initialDelayMillis = 500,
    spacing = MarqueeSpacing(24.dp),
    velocity = 24.dp,
)

@Composable
private fun CurrentConditions(
    uiState: WeatherUiState,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    // Hero temperature scales down a notch on small (≤195dp) watches so it never clips.
    val isSmallScreen = LocalConfiguration.current.screenWidthDp <= 195
    val heroStyle =
        if (isSmallScreen) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium
    val iconSize = if (isSmallScreen) 40.dp else 48.dp

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = uiState.location,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            color = textColor.copy(alpha = 0.7f),
            modifier = marqueeModifier,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val lottieRes = uiState.icon.toLottieRes(uiState.dayPhase)
            if (lottieRes != null) {
                WeatherOverlay(res = lottieRes, modifier = Modifier.size(iconSize))
            } else {
                Icon(
                    imageVector = uiState.icon.toImageVector(),
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(iconSize),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = uiState.temperature,
                style = heroStyle,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
        // Condition + feels-like share one line so the "what's it actually like"
        // context doesn't cost the hero an extra row of height.
        val conditionLine = listOfNotNull(
            uiState.condition,
            uiState.feelsLike?.let { "Feels $it" },
        ).joinToString("  •  ")
        if (conditionLine.isNotEmpty()) {
            Text(
                text = conditionLine,
                style = MaterialTheme.typography.bodyExtraSmall,
                maxLines = 1,
                softWrap = false,
                color = textColor.copy(alpha = 0.8f),
                modifier = marqueeModifier,
            )
        }
        if (uiState.highTemp != null || uiState.lowTemp != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                uiState.highTemp?.let {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(10.dp),
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                }
                if (uiState.highTemp != null && uiState.lowTemp != null) {
                    Spacer(Modifier.width(2.dp))
                }
                uiState.lowTemp?.let {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(10.dp),
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                }
            }
        }
    }
}

// Three columns fit inside the safe area of the smallest round face; each LazyRow
// item takes exactly a third of the viewport so a fling always lands on a full page.
private const val VISIBLE_HOURS = 3

// The strip rides the same "⌢" arc as its scroll indicator: columns sink and tilt the
// further they are from the viewport centre, so hours appear to surface from the
// bottom edge of the dial instead of sliding along a straight line. Kept subtle —
// this is a glance strip, not a showpiece.
private val HourlyArcDepth = 5.dp
private const val HOURLY_ARC_MAX_TILT_DEGREES = 7f

@Composable
private fun HourlyStrip(
    hours: List<HourlyUiState>,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(hours) { index, hour ->
                // The soonest hour is the one glance actually cares about, so its chip
                // reads slightly stronger than the rest — a static index check, not derived state.
                val isNearest = index == 0
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillParentMaxWidth(1f / VISIBLE_HOURS)
                        // Parabolic offset matching the indicator's quadratic: y grows
                        // with the square of the distance from centre, and the column
                        // tilts tangent to that curve. listState is read inside the
                        // graphicsLayer block, so scrolling only re-draws the layer —
                        // it never recomposes the row.
                        .graphicsLayer {
                            val layoutInfo = listState.layoutInfo
                            val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                ?: return@graphicsLayer
                            val viewportCenter =
                                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                            val halfViewport =
                                (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
                            val itemCenter = item.offset + item.size / 2f
                            val normalized =
                                ((itemCenter - viewportCenter) / halfViewport).coerceIn(-1f, 1f)
                            translationY = HourlyArcDepth.toPx() * normalized * normalized
                            rotationZ = HOURLY_ARC_MAX_TILT_DEGREES * normalized
                        }
                        .padding(horizontal = 2.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(textColor.copy(alpha = if (isNearest) 0.16f else 0.07f))
                            .padding(vertical = 4.dp),
                    ) {
                        Text(
                            text = hour.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.75f),
                        )
                        Icon(
                            imageVector = hour.icon.toImageVector(),
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = hour.temperature,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        )
                    }
                }
            }
        }
        if (hours.size > VISIBLE_HOURS) {
            HourlyScrollIndicator(
                listState = listState,
                itemCount = hours.size,
                color = textColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .fillMaxWidth(0.38f)
                    .height(6.dp),
            )
        }
    }
}

/**
 * Horizontal scroll indicator drawn as a "⌢" cap arc — the line bows upward at its
 * midpoint — with a thumb segment that slides along
 * the curve as the strip scrolls. All state reads happen inside the draw phase, so
 * scrolling only ever redraws this Canvas, never recomposes it.
 */
@Composable
private fun HourlyScrollIndicator(
    listState: LazyListState,
    itemCount: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val trackPath = remember { Path() }
    val thumbPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }
    Canvas(modifier = modifier) {
        val strokeWidth = 2.5.dp.toPx()
        val startY = size.height - strokeWidth
        val riseHeight = size.height - strokeWidth * 2
        trackPath.reset()
        trackPath.moveTo(0f, startY)
        // "⌢" cap: ends sit low, the quadratic rises to startY - riseHeight at its
        // midpoint (half the control offset).
        trackPath.quadraticTo(size.width / 2f, startY - riseHeight * 2f, size.width, startY)
        drawPath(
            path = trackPath,
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        val layoutInfo = listState.layoutInfo
        val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return@Canvas
        val viewport = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val scrollableRange = (itemSize * itemCount - viewport).coerceAtLeast(1)
        val scrolled =
            listState.firstVisibleItemIndex * itemSize + listState.firstVisibleItemScrollOffset
        val progress = (scrolled.toFloat() / scrollableRange).coerceIn(0f, 1f)

        pathMeasure.setPath(trackPath, false)
        val trackLength = pathMeasure.length
        val thumbLength = trackLength * (VISIBLE_HOURS.toFloat() / itemCount)
        val thumbStart = (trackLength - thumbLength) * progress
        thumbPath.reset()
        pathMeasure.getSegment(thumbStart, thumbStart + thumbLength, thumbPath, true)
        drawPath(
            path = thumbPath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun AlertEdgeButton(
    alert: AlertUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EdgeButton(
        onClick = onClick,
        modifier = modifier,
        buttonSize = EdgeButtonSize.Small,
        colors = ButtonDefaults.buttonColors(
            containerColor = AlertAmber.copy(alpha = 0.2f),
            contentColor = AlertAmber,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = alert.event,
            style = MaterialTheme.typography.bodyExtraSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun WeatherIconType.toImageVector(): ImageVector =
    when (this) {
        WeatherIconType.CLEAR -> Icons.Filled.WbSunny
        WeatherIconType.CLOUDS -> Icons.Filled.Cloud
        WeatherIconType.RAIN -> Icons.Filled.Grain
        WeatherIconType.THUNDERSTORM -> Icons.Filled.Bolt
        WeatherIconType.SNOW -> Icons.Filled.AcUnit
        WeatherIconType.ATMOSPHERE -> Icons.Filled.Dehaze
        WeatherIconType.WIND -> Icons.Filled.Air
        WeatherIconType.UNKNOWN -> Icons.Filled.WbCloudy
    }

private fun gradient(top: Long, bottom: Long): Brush =
    Brush.verticalGradient(listOf(Color(top), Color(bottom)))

// Backgrounds are keyed by condition AND time of day. Every variant runs dark at the
// bottom stop — near black so the dial blends into the bezel, stays AMOLED-cheap, and
// light text never loses contrast. DAY carries the strongest condition tint at the top,
// TWILIGHT warms everything toward sunrise/sunset amber, and NIGHT collapses to a faint
// tint over black so the screen doesn't glare in a dark room.
internal fun WeatherIconType.toBackgroundGradient(phase: DayPhase): Brush =
    when (phase) {
        DayPhase.DAY -> when (this) {
            WeatherIconType.CLEAR -> gradient(0xFF1565C0, 0xFF071426)
            WeatherIconType.CLOUDS -> gradient(0xFF56707E, 0xFF0E1417)
            WeatherIconType.RAIN -> gradient(0xFF3B5361, 0xFF0A1116)
            WeatherIconType.THUNDERSTORM -> gradient(0xFF45338F, 0xFF0C0817)
            WeatherIconType.SNOW -> gradient(0xFF4E7FA0, 0xFF0D161D)
            WeatherIconType.ATMOSPHERE -> gradient(0xFF5E6B70, 0xFF0F1213)
            WeatherIconType.WIND -> gradient(0xFF3E7C74, 0xFF0A1413)
            WeatherIconType.UNKNOWN -> gradient(0xFF4A545C, 0xFF0D0F11)
        }

        DayPhase.TWILIGHT -> when (this) {
            WeatherIconType.CLEAR -> gradient(0xFF9C4A14, 0xFF170905)
            WeatherIconType.CLOUDS -> gradient(0xFF7A4A33, 0xFF120B08)
            WeatherIconType.RAIN -> gradient(0xFF5C4438, 0xFF0E0B09)
            WeatherIconType.THUNDERSTORM -> gradient(0xFF6A2E63, 0xFF120714)
            WeatherIconType.SNOW -> gradient(0xFF8A5A66, 0xFF130D10)
            WeatherIconType.ATMOSPHERE -> gradient(0xFF6E5648, 0xFF100C0A)
            WeatherIconType.WIND -> gradient(0xFF6B5A3C, 0xFF0F0D08)
            WeatherIconType.UNKNOWN -> gradient(0xFF6E4A3A, 0xFF100B08)
        }

        DayPhase.NIGHT -> when (this) {
            WeatherIconType.CLEAR -> gradient(0xFF101E3C, 0xFF000000)
            WeatherIconType.CLOUDS -> gradient(0xFF171D22, 0xFF000000)
            WeatherIconType.RAIN -> gradient(0xFF0F1B24, 0xFF000000)
            WeatherIconType.THUNDERSTORM -> gradient(0xFF1B0F2E, 0xFF000000)
            WeatherIconType.SNOW -> gradient(0xFF14202B, 0xFF000000)
            WeatherIconType.ATMOSPHERE -> gradient(0xFF191C1D, 0xFF000000)
            WeatherIconType.WIND -> gradient(0xFF12201E, 0xFF000000)
            WeatherIconType.UNKNOWN -> gradient(0xFF141618, 0xFF000000)
        }
    }

// Text/icon tint per condition and phase — every value stays well above 7:1 contrast on
// its gradient's darkest stop. DAY uses bright condition pastels, TWILIGHT warms them
// toward cream, and NIGHT softens luminance a notch so text doesn't bloom in low light.
internal fun WeatherIconType.toTextColor(phase: DayPhase): Color =
    when (phase) {
        DayPhase.DAY -> when (this) {
            WeatherIconType.CLEAR -> Color(0xFFFFF9C4)
            WeatherIconType.CLOUDS -> Color(0xFFECEFF1)
            WeatherIconType.RAIN -> Color(0xFFB3E5FC)
            WeatherIconType.THUNDERSTORM -> Color(0xFFE1BEE7)
            WeatherIconType.SNOW -> Color(0xFFFFFFFF)
            WeatherIconType.ATMOSPHERE -> Color(0xFFCFD8DC)
            WeatherIconType.WIND -> Color(0xFFB2DFDB)
            WeatherIconType.UNKNOWN -> Color(0xFFECEFF1)
        }

        DayPhase.TWILIGHT -> when (this) {
            WeatherIconType.CLEAR -> Color(0xFFFFE0B2)
            WeatherIconType.CLOUDS -> Color(0xFFFFE8D6)
            WeatherIconType.RAIN -> Color(0xFFF5DCC8)
            WeatherIconType.THUNDERSTORM -> Color(0xFFF0D5F5)
            WeatherIconType.SNOW -> Color(0xFFFFEBEE)
            WeatherIconType.ATMOSPHERE -> Color(0xFFF0E4DA)
            WeatherIconType.WIND -> Color(0xFFF5EBD0)
            WeatherIconType.UNKNOWN -> Color(0xFFFFE8D6)
        }

        DayPhase.NIGHT -> when (this) {
            WeatherIconType.CLEAR -> Color(0xFFD6E4FF)
            WeatherIconType.CLOUDS -> Color(0xFFDCE3E8)
            WeatherIconType.RAIN -> Color(0xFFC9E4F5)
            WeatherIconType.THUNDERSTORM -> Color(0xFFDCC8EE)
            WeatherIconType.SNOW -> Color(0xFFEAF4FA)
            WeatherIconType.ATMOSPHERE -> Color(0xFFD8DDDF)
            WeatherIconType.WIND -> Color(0xFFC8E6E2)
            WeatherIconType.UNKNOWN -> Color(0xFFDDE1E4)
        }
    }

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
@Preview(device = WearDevices.SQUARE)
@Composable
private fun WeatherScreenPreview() {
    AppScaffold {
        WeatherScreen(
            uiState = WeatherUiMapper.mapToUiState(mockWeatherForecast, mockLocationName),
            onAlertClick = {},
        )
    }
}
