package bose.ankush.home.presentation.wander

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Bottom sheet drawn in the home layout, not in a window. The caller bounds it above the
 * tab bar, so the tabs stay tappable and the system bars and photo never change. One
 * visibility drives both the scrim fade and the card slide, on the same tween. Long content
 * opens at a peek height and drags or scrolls up to the full area. [header] sits under the
 * drag handle and drags the card; [body] fills the rest.
 */
@Composable
internal fun WanderSheetPanel(
    visible: Boolean,
    spec: WanderSheetSpec,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(enabled = visible, onBack = spec.onDismiss)
    AnimatedVisibility(
        visible = visible,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().clipToBounds()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .animateEnterExit(enter = fadeIn(panelEnter()), exit = fadeOut(panelExit()))
                        .background(PanelScrim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = spec.closeLabel,
                            onClick = spec.onDismiss,
                        ),
            )
            val fixedHeight = spec.heightFraction?.let { Modifier.height(maxHeight * it) } ?: Modifier
            SheetCard(
                spec = spec,
                peekHeight = spec.peekFraction?.let { maxHeight * it },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .heightIn(max = maxHeight)
                        .then(fixedHeight)
                        .animateEnterExit(
                            enter = slideInVertically(panelEnter()) { it } + fadeIn(panelEnter()),
                            exit = slideOutVertically(panelExit()) { it } + fadeOut(panelExit()),
                        ),
                header = header,
                body = body,
            )
        }
    }
}

@Composable
private fun SheetCard(
    spec: WanderSheetSpec,
    peekHeight: Dp?,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    val peekPx = peekHeight?.let { with(LocalDensity.current) { it.toPx() } } ?: Float.POSITIVE_INFINITY
    val scope = rememberCoroutineScope()
    val sheet = remember(spec.key) { SheetOffset(scope) }
    val dragState = rememberDraggableState { delta -> sheet.drag(delta) }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .onSizeChanged { size -> sheet.measured(size.height, peekPx) }
                .alpha(if (sheet.isMeasured) 1f else 0f)
                .padding(top = 24.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
                .offset { IntOffset(0, sheet.offset.floatValue.roundToInt()) }
                .clip(RoundedCornerShape(20.dp))
                .background(WanderSheetFill)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ).nestedScroll(remember(sheet) { SheetScroll(sheet) }),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity -> sheet.release(velocity, spec.onDismiss) },
                    ).padding(start = 20.dp, end = 8.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .background(WanderSheetMuted.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
            )
            header()
        }
        body()
    }
}

/**
 * Vertical position of the sheet card: 0 is fully open, [peek] shows the peek fraction
 * of the area, and anything past [peek] is a drag toward dismissing. Short content has no peek.
 */
private class SheetOffset(
    private val scope: CoroutineScope,
) {
    val offset: MutableFloatState = mutableFloatStateOf(0f)
    var peek: Float = 0f
        private set
    var isMeasured by mutableStateOf(false)
        private set
    private var settling: Job? = null

    fun measured(
        heightPx: Int,
        peekHeightPx: Float,
    ) {
        val newPeek = if (heightPx > peekHeightPx * PEEK_MIN_RATIO) heightPx - peekHeightPx else 0f
        if (!isMeasured) {
            offset.floatValue = newPeek
            isMeasured = true
        } else if (offset.floatValue == peek) {
            offset.floatValue = newPeek
        }
        peek = newPeek
    }

    fun drag(delta: Float) {
        settling?.cancel()
        offset.floatValue = (offset.floatValue + delta).coerceAtLeast(0f)
    }

    /** Moves the card up to open, down to the peek; returns the part of [delta] it used. */
    fun dragWithin(delta: Float): Float {
        val current = offset.floatValue
        val next = (current + delta).coerceIn(0f, peek)
        if (next != current) {
            settling?.cancel()
            offset.floatValue = next
        }
        return next - current
    }

    fun release(
        velocity: Float,
        onDismiss: () -> Unit,
    ) {
        val current = offset.floatValue
        when {
            current > peek + DISMISS_DRAG_PX -> onDismiss()
            velocity > DISMISS_VELOCITY && current >= peek -> onDismiss()
            else -> settleTo(target(current, velocity))
        }
    }

    fun target(
        current: Float,
        velocity: Float,
    ): Float =
        when {
            velocity < -SETTLE_VELOCITY -> 0f
            velocity > SETTLE_VELOCITY -> peek
            abs(current) < abs(current - peek) -> 0f
            else -> peek
        }

    fun settleTo(target: Float) {
        settling?.cancel()
        val start = offset.floatValue
        settling =
            scope.launch {
                animate(start, target, animationSpec = panelEnter()) { value, _ -> offset.floatValue = value }
            }
    }
}

/**
 * Scrolling the body first opens the card, then scrolls the content; at the top, pulling down
 * lowers it back to the peek. Everything left over is consumed so the home column and pull to
 * refresh never move.
 */
private class SheetScroll(
    private val sheet: SheetOffset,
) : NestedScrollConnection {
    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset = if (available.y < 0f) Offset(0f, sheet.dragWithin(available.y)) else Offset.Zero

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (available.y > 0f && source == NestedScrollSource.UserInput) sheet.dragWithin(available.y)
        return available
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val current = sheet.offset.floatValue
        if (current <= 0f || current >= sheet.peek) return Velocity.Zero
        sheet.settleTo(sheet.target(current, available.y))
        return available
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity = available
}

private fun <T> panelEnter() = tween<T>(durationMillis = PANEL_ENTER_MILLIS, easing = FastOutSlowInEasing)

private fun <T> panelExit() = tween<T>(durationMillis = PANEL_EXIT_MILLIS, easing = FastOutLinearInEasing)

internal val WanderSheetFill = Color(0xFF14171C)
internal val WanderSheetMuted = WanderOnDark.copy(alpha = 0.6f)
private val PanelScrim =
    Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.04f),
        1f to Color.Black.copy(alpha = 0.12f),
    )
private const val PANEL_ENTER_MILLIS = 220
private const val PANEL_EXIT_MILLIS = 180
private const val PEEK_MIN_RATIO = 1.1f
private const val DISMISS_VELOCITY = 1500f
private const val SETTLE_VELOCITY = 600f
private const val DISMISS_DRAG_PX = 120f
