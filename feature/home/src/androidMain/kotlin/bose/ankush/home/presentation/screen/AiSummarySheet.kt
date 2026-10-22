package bose.ankush.home.presentation.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.ToastOnWarning
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.ai_summary_close
import bose.ankush.home.generated.resources.ai_summary_failed
import bose.ankush.home.generated.resources.ai_summary_for_place
import bose.ankush.home.generated.resources.ai_summary_loading
import bose.ankush.home.generated.resources.ai_summary_privacy
import bose.ankush.home.generated.resources.ai_summary_retry
import bose.ankush.home.generated.resources.ai_summary_tips_heading
import bose.ankush.home.generated.resources.ai_summary_title
import bose.ankush.home.generated.resources.ai_summary_writing
import bose.ankush.home.presentation.ai.AiSummaryPhase
import bose.ankush.home.presentation.ai.AiSummaryTip
import bose.ankush.home.presentation.ai.AiSummaryTipKind
import bose.ankush.home.presentation.ai.AiSummaryUiState
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

/** What the summary card and sheet need from [bose.ankush.home.presentation.ai.AiSummaryViewModel]. */
internal class AiSummaryBinding(
    val state: AiSummaryUiState = AiSummaryUiState(),
    /** False hides the home card entirely: no model on this device, or no forecast to summarize yet. */
    val isAvailable: Boolean = false,
    /** The sheet just opened: start generating. */
    val onOpen: () -> Unit = {},
    /** The user tapped "Try again" after a failure. */
    val onRetry: () -> Unit = {},
    /** The sheet closed: stop generating. */
    val onDismiss: () -> Unit = {},
)

/**
 * Weather summary in a [SheetPanel], on the same chrome as the other home sheets. Draws whatever
 * [AiSummaryUiState] says and nothing else: placeholder lines, the text as it arrives, tip chips,
 * or a retry.
 */
@Composable
internal fun AiSummarySheet(
    visible: Boolean,
    binding: AiSummaryBinding,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val closeLabel = stringResource(Res.string.ai_summary_close)
    SheetPanel(
        visible = visible,
        spec = SheetPanelSpec(onDismiss = onDismiss, closeLabel = closeLabel),
        modifier = modifier,
        header = {
            SheetTitleBar(
                title = stringResource(Res.string.ai_summary_title),
                closeLabel = closeLabel,
                onClose = onDismiss,
            )
        },
        body = { AiSummaryBody(state = binding.state, onRetry = binding.onRetry) },
    )
}

@Composable
private fun AiSummaryBody(
    state: AiSummaryUiState,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.place?.takeIf { it.isNotBlank() }?.let { place ->
            SheetSubtitle(text = stringResource(Res.string.ai_summary_for_place, place))
        }
        when (state.phase) {
            AiSummaryPhase.Loading -> LoadingLines()
            AiSummaryPhase.Ready -> SummaryText(text = state.text, isStreaming = state.isStreaming)
            AiSummaryPhase.Failed -> FailedBlock(onRetry = onRetry)
        }
        if (state.phase == AiSummaryPhase.Ready && state.tips.isNotEmpty()) {
            TipsBlock(tips = state.tips)
        }
        Text(
            text = stringResource(Res.string.ai_summary_privacy),
            color = SheetMutedText,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun SummaryText(
    text: String,
    isStreaming: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = text, color = ContentOnDark, fontSize = 17.sp, lineHeight = 25.sp)
        if (isStreaming) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LeafSpinner(size = 22.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.ai_summary_writing),
                    color = SheetMutedText,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/** The leaf thinking, then placeholder lines with a light sweeping across while nothing has arrived. */
@Composable
private fun LoadingLines() {
    val sweep by rememberInfiniteTransition(label = "ai-summary-shimmer").animateFloat(
        initialValue = -SHIMMER_SPAN,
        targetValue = SHIMMER_END,
        animationSpec = infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing)),
        label = "sweep",
    )
    val brush =
        Brush.linearGradient(
            colors =
                listOf(
                    SheetFieldFill,
                    WarningYellow.copy(alpha = SHIMMER_ALPHA),
                    SheetFieldFill,
                ),
            start = Offset(sweep, 0f),
            end = Offset(sweep + SHIMMER_SPAN, 0f),
        )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LeafSpinner(size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.ai_summary_loading),
                color = SheetMutedText,
                fontSize = 14.sp,
            )
        }
        LOADING_BAR_WIDTHS.forEach { width ->
            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth(width)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(brush),
            )
        }
    }
}

/**
 * The app's leaf being made: a multicolor comet circles it while the leaf draws its outline,
 * then its vein, blooms into colour, and fades to start again. Used while the summary is written.
 */
@Composable
private fun LeafSpinner(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "leaf")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeat(RING_SPIN_MILLIS),
        label = "spin",
    )
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeat(LEAF_CYCLE_MILLIS),
        label = "cycle",
    )
    val measure = remember { PathMeasure() }
    val segment = remember { Path() }
    Canvas(modifier = modifier.size(size)) {
        drawRing(spin)
        drawLeaf(cycle, measure, segment)
    }
}

private fun infiniteRepeat(millis: Int) = infiniteRepeatable<Float>(tween(millis, easing = LinearEasing))

private fun DrawScope.drawRing(spin: Float) {
    val stroke = size.minDimension * RING_STROKE
    val inset = stroke * RING_GLOW_STROKES / 2
    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
    val brush = Brush.sweepGradient(colorStops = RING_STOPS, center = center)
    rotate(spin) {
        drawArc(
            brush = brush,
            startAngle = 0f,
            sweepAngle = RING_SWEEP,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            alpha = RING_GLOW_ALPHA,
            style = Stroke(width = stroke * RING_GLOW_STROKES, cap = StrokeCap.Round),
        )
        drawArc(
            brush = brush,
            startAngle = 0f,
            sweepAngle = RING_SWEEP,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawLeaf(
    cycle: Float,
    measure: PathMeasure,
    segment: Path,
) {
    val side = size.minDimension * LEAF_SHARE
    val origin = Offset((size.width - side) / 2, (size.height - side) / 2)
    val outline = ease(phase(cycle, 0f, DRAW_END))
    val vein = ease(phase(cycle, VEIN_START, VEIN_END))
    val bloom = ease(phase(cycle, BLOOM_START, BLOOM_END))
    val fade = 1f - phase(cycle, FADE_START, 1f)
    val pop = LEAF_POP_FROM + (1f - LEAF_POP_FROM) * bloom
    val sway = sin(cycle * SWAY_TURNS * 2f * PI.toFloat()) * LEAF_SWAY_DEGREES * bloom
    val strokeWidth = size.minDimension * LEAF_STROKE
    val body = leafBody(side, origin)
    val rib = leafVein(side, origin)
    rotate(sway, pivot = center) {
        scale(pop, pivot = center) {
            measure.setPath(body, forceClosed = false)
            segment.rewind()
            measure.getSegment(0f, measure.length * outline, segment, startWithMoveTo = true)
            drawPath(
                segment,
                WarningYellow,
                alpha = fade,
                style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(body, WarningYellow, alpha = bloom * fade)
            measure.setPath(rib, forceClosed = false)
            segment.rewind()
            measure.getSegment(0f, measure.length * vein, segment, startWithMoveTo = true)
            val ribColor = lerp(WarningYellow, ToastOnWarning, bloom)
            drawPath(
                segment,
                ribColor,
                alpha = fade,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

/** Leaf outline, base at the lower left and tip at the upper right, in a [side] square at [origin]. */
private fun leafBody(
    side: Float,
    origin: Offset,
): Path {
    fun at(
        x: Float,
        y: Float,
    ) = Offset(origin.x + x * side, origin.y + y * side)
    return Path().apply {
        moveTo(at(0.22f, 0.78f))
        cubicTo(at(0.12f, 0.38f), at(0.45f, 0.15f), at(0.82f, 0.18f))
        cubicTo(at(0.85f, 0.55f), at(0.58f, 0.88f), at(0.22f, 0.78f))
        close()
    }
}

/** Stem and midrib, from outside the base up toward the tip. */
private fun leafVein(
    side: Float,
    origin: Offset,
): Path {
    fun at(
        x: Float,
        y: Float,
    ) = Offset(origin.x + x * side, origin.y + y * side)
    return Path().apply {
        moveTo(at(0.1f, 0.9f))
        lineTo(at(0.62f, 0.38f))
    }
}

private fun Path.moveTo(p: Offset) = moveTo(p.x, p.y)

private fun Path.lineTo(p: Offset) = lineTo(p.x, p.y)

private fun Path.cubicTo(
    a: Offset,
    b: Offset,
    c: Offset,
) = cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)

/** 0 before [from], 1 after [to], linear between. */
private fun phase(
    t: Float,
    from: Float,
    to: Float,
): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)

private fun ease(t: Float): Float = FastOutSlowInEasing.transform(t)

@Composable
private fun TipsBlock(tips: List<AiSummaryTip>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(Res.string.ai_summary_tips_heading),
            color = ContentOnDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tips.forEach { TipChip(it) }
        }
    }
}

@Composable
private fun TipChip(tip: AiSummaryTip) {
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(SheetFieldFill)
                .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = tip.kind.icon(),
            contentDescription = null,
            tint = WarningYellow,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = tip.label, color = ContentOnDark, fontSize = 14.sp)
    }
}

@Composable
private fun FailedBlock(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = stringResource(Res.string.ai_summary_failed),
            color = ContentOnDark,
            fontSize = 16.sp,
        )
        SheetPrimaryButton(text = stringResource(Res.string.ai_summary_retry), onClick = onRetry)
    }
}

private fun AiSummaryTipKind.icon(): ImageVector =
    when (this) {
        AiSummaryTipKind.Umbrella -> Icons.Filled.Umbrella
        AiSummaryTipKind.Sunscreen -> Icons.Filled.WbSunny
        AiSummaryTipKind.Wind -> Icons.Filled.Air
        AiSummaryTipKind.Jacket -> Icons.Filled.Checkroom
    }

private val LOADING_BAR_WIDTHS = listOf(1f, 0.92f, 0.6f)
private const val SHIMMER_MILLIS = 1400
private const val SHIMMER_SPAN = 500f
private const val SHIMMER_END = 1600f
private const val SHIMMER_ALPHA = 0.28f
private const val FULL_TURN = 360f
private const val RING_SPIN_MILLIS = 2200
private const val RING_STROKE = 0.07f
private const val RING_GLOW_STROKES = 3f
private const val RING_GLOW_ALPHA = 0.22f
private const val RING_SWEEP = 300f
private const val LEAF_CYCLE_MILLIS = 3200
private const val LEAF_SHARE = 0.56f
private const val LEAF_STROKE = 0.05f
private const val LEAF_POP_FROM = 0.88f
private const val LEAF_SWAY_DEGREES = 6f
private const val SWAY_TURNS = 2f
private const val DRAW_END = 0.4f
private const val VEIN_START = 0.22f
private const val VEIN_END = 0.5f
private const val BLOOM_START = 0.4f
private const val BLOOM_END = 0.58f
private const val FADE_START = 0.88f

/** Comet colours from a transparent tail to a bright head; the stops stop at the arc's end (300/360). */
private val RING_STOPS =
    arrayOf(
        0f to Color(0xFF4285F4).copy(alpha = 0f),
        0.15f to Color(0xFF4285F4),
        0.4f to Color(0xFF9B72CB),
        0.6f to Color(0xFFD96570),
        0.76f to WarningYellow,
        0.83f to Color(0xFF34A853),
        1f to Color(0xFF34A853).copy(alpha = 0f),
    )
