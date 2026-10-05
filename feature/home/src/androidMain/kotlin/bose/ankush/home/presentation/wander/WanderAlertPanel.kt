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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.domain.model.WeatherForecast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Alert details drawn in the home layout, not in a window. The caller bounds it above
 * the tab bar, so the tabs stay tappable and the system bars and photo never change.
 * One visibility drives both the scrim fade and the panel slide, on the same tween.
 * Long content opens at a peek height and drags or scrolls up to the full area.
 */
@Composable
internal fun WanderAlertPanel(
    alert: WeatherForecast.Alert?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Plain holder, not state: keeps the last alert on screen while the panel slides out
    // without a write during composition.
    val lastAlert = remember { arrayOfNulls<WeatherForecast.Alert>(1) }
    if (alert != null) lastAlert[0] = alert
    val shown = alert ?: lastAlert[0]
    BackHandler(enabled = alert != null, onBack = onDismiss)
    AnimatedVisibility(
        visible = alert != null,
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
                            onClickLabel = "Close alert details",
                            onClick = onDismiss,
                        ),
            )
            if (shown != null) {
                AlertPanelCard(
                    alert = shown,
                    onDismiss = onDismiss,
                    peekHeight = maxHeight * PANEL_PEEK_FRACTION,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .heightIn(max = maxHeight)
                            .animateEnterExit(
                                enter = slideInVertically(panelEnter()) { it } + fadeIn(panelEnter()),
                                exit = slideOutVertically(panelExit()) { it } + fadeOut(panelExit()),
                            ),
                )
            }
        }
    }
}

@Composable
private fun AlertPanelCard(
    alert: WeatherForecast.Alert,
    onDismiss: () -> Unit,
    peekHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val title = alert.event?.takeIf { it.isNotBlank() } ?: ALERT_ROW_FALLBACK_TITLE
    val parsed = remember(alert.description) { parseAlertDescription(alert.description) }
    val peekPx = with(LocalDensity.current) { peekHeight.toPx() }
    val scope = rememberCoroutineScope()
    val sheet = remember(alert) { SheetOffset(scope) }
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
                .background(SheetFill)
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
                        onDragStopped = { velocity -> sheet.release(velocity, onDismiss) },
                    ).padding(start = 20.dp, end = 8.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .background(Muted.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = AlertYellow,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = title,
                    color = WanderOnDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close alert details",
                        tint = WanderOnDark,
                    )
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AlertTimes(alert = alert, summary = parsed.summary)
            parsed.displaySections().forEach { display -> AlertSectionBlock(display) }
            parsed.fallback?.let { Text(text = it, color = WanderOnDark, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun AlertTimes(
    alert: WeatherForecast.Alert,
    summary: String?,
) {
    val duration: String =
        "${alert.start?.toIssuedLabel()} - ${alert.end?.toIssuedLabel()}".ifBlank { "Still unknown" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                color = WanderOnDark,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AlertChip(duration.ifEmpty { "Timeline unknown" })
            AlertChip(alert.sender_name ?: "Source unknown")
        }
    }
}

@Composable
private fun AlertChip(text: String) {
    Text(
        text = text,
        color = WanderOnDark,
        fontSize = 12.sp,
        maxLines = 1,
        modifier =
            Modifier
                .background(ChipFill, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun AlertSectionBlock(display: AlertDisplaySection) {
    val section = display.section
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        display.heading?.let { label ->
            Text(
                text = label.uppercase(),
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
            )
        }
        val items = section.items
        if (items != null) {
            AreaList(items)
        } else {
            Text(text = section.body, color = WanderOnDark, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AreaList(items: List<String>) {
    var showAll by remember(items) { mutableStateOf(false) }
    val visible = if (showAll) items else items.take(AREA_PREVIEW)
    visible.forEach { area ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(4.dp).background(Muted, CircleShape))
            Text(
                text = area,
                color = WanderOnDark,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    if (items.size > AREA_PREVIEW) {
        Text(
            text = if (showAll) "Show less" else "Show all (${items.size})",
            color = WanderOnDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { showAll = !showAll }
                    .padding(vertical = 4.dp),
        )
    }
}

/**
 * Vertical position of the panel card: 0 is fully open, [peek] shows [PANEL_PEEK_FRACTION]
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
 * Scrolling the details first opens the card, then scrolls the text; at the top, pulling down
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

private val SheetFill = Color(0xFF14171C)
private val Muted = WanderOnDark.copy(alpha = 0.6f)
private val ChipFill = WanderOnDark.copy(alpha = 0.12f)
private val PanelScrim =
    Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.04f),
        1f to Color.Black.copy(alpha = 0.12f),
    )
private const val PANEL_ENTER_MILLIS = 220
private const val PANEL_EXIT_MILLIS = 180
private const val PANEL_PEEK_FRACTION = 0.55f
private const val PEEK_MIN_RATIO = 1.1f
private const val DISMISS_VELOCITY = 1500f
private const val SETTLE_VELOCITY = 600f
private const val DISMISS_DRAG_PX = 120f
private const val AREA_PREVIEW = 4
