package bose.ankush.home.presentation.wander

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.domain.model.WeatherForecast
import kotlin.math.roundToInt

/**
 * Alert details drawn in the home layout, not in a window. The caller bounds it above
 * the tab bar, so the tabs stay tappable and the system bars and photo never change.
 * One visibility drives both the scrim fade and the panel slide, on the same tween.
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .heightIn(max = maxHeight * PANEL_MAX_HEIGHT_FRACTION)
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
    modifier: Modifier = Modifier,
) {
    val title = alert.event?.takeIf { it.isNotBlank() } ?: ALERT_ROW_FALLBACK_TITLE
    val parsed = remember(alert.description) { parseAlertDescription(alert.description) }
    var dragOffset by remember(alert) { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta -> dragOffset = (dragOffset + delta).coerceAtLeast(0f) }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
                .offset { IntOffset(0, dragOffset.roundToInt()) }
                .clip(RoundedCornerShape(20.dp))
                .background(SheetFill)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ).nestedScroll(remember { ConsumeAllScroll() }),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = {
                            if (dragOffset > DISMISS_DRAG_PX) onDismiss() else dragOffset = 0f
                        },
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
            alert.sender_name?.takeIf { it.isNotBlank() }?.let { source ->
                Text(text = "Source · $source", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AlertTimes(
    alert: WeatherForecast.Alert,
    summary: String?,
) {
    val starts = alert.start?.toIssuedLabel()
    val until = alert.end?.toIssuedLabel()
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
        if (starts != null || until != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                starts?.let { AlertChip("Starts $it") }
                until?.let { AlertChip("Until $it") }
            }
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

/** Keeps panel scrolling from reaching the home column or pull to refresh. */
private class ConsumeAllScroll : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = available

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
private const val PANEL_MAX_HEIGHT_FRACTION = 0.85f
private const val DISMISS_DRAG_PX = 120f
private const val AREA_PREVIEW = 4
