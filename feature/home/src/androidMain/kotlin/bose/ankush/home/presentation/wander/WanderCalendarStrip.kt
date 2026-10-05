package bose.ankush.home.presentation.wander

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.daily_forecast_heading_txt
import bose.ankush.home.generated.resources.ic_sunny
import bose.ankush.home.presentation.shell.ShellSectionKind
import bose.ankush.home.presentation.shell.retryContentDescription
import bose.ankush.home.presentation.util.getIconUrl
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WanderCalendarStrip(
    model: WanderCalendarStripModel,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = WanderOnDark,
    onRetryCalendar: (() -> Unit)? = null,
) {
    val days = model.days
    val eventDates = model.eventDates
    val showWeekShimmer = model.showWeekShimmer
    if (days.isEmpty() && !showWeekShimmer && onRetryCalendar == null) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.daily_forecast_heading_txt),
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = CALENDAR,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = CARD_ALPHA))
                        .semantics { contentDescription = ADD_EVENT }
                        .clickable(role = Role.Button, onClick = onOpen)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            if (onRetryCalendar != null) {
                WanderActionLabel(
                    label = retryContentDescription(ShellSectionKind.Calendar),
                    onClick = onRetryCalendar,
                    contentColor = contentColor,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (showWeekShimmer && days.isEmpty()) {
            WeekShimmer()
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                days.forEach { day ->
                    DayCell(
                        day = day,
                        marked = day.day != null && day.day in eventDates,
                        contentColor = contentColor,
                        onOpen = onOpen,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: WanderCalendarDay,
    marked: Boolean,
    contentColor: Color,
    onOpen: () -> Unit,
) {
    val range = day.rangeLabel()
    Column(
        modifier =
            Modifier
                .width(DayWidth)
                .semantics { contentDescription = day.eventDescription() }
                .clickable(role = Role.Button, onClick = onOpen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = day.label, color = contentColor, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (day.selected) Color.White else Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day.date,
                color = if (day.selected) WanderOnLight else contentColor,
                fontSize = 16.sp,
                fontWeight = if (day.selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        if (marked) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(EventDot),
            )
        }
        if (day.icon.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            AsyncImage(
                model = day.icon.getIconUrl(),
                placeholder = painterResource(Res.drawable.ic_sunny),
                error = painterResource(Res.drawable.ic_sunny),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(28.dp),
            )
        }
        if (range.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = range,
                color = contentColor,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (day.description.isNotBlank()) {
            Text(
                text = day.description,
                color = contentColor,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeekShimmer() {
    val brush = rememberWanderShimmerBrush()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(SHIMMER_DAYS) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 28.dp, height = 10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(brush),
                )
            }
        }
    }
}

@Composable
internal fun rememberWanderShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "wander-shimmer")
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "wander-shimmer-travel",
    )
    val base = Color.White.copy(alpha = 0.10f)
    val highlight = Color.White.copy(alpha = 0.34f)
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x = travel * SHIMMER_TRAVEL, y = 0f),
        end = Offset(x = travel * SHIMMER_TRAVEL + SHIMMER_SPAN, y = SHIMMER_DROP),
    )
}

@Composable
internal fun WanderActionLabel(
    label: String,
    onClick: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = contentColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = CARD_ALPHA))
                .semantics { contentDescription = label }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

private fun WanderCalendarDay.rangeLabel(): String =
    when {
        min.isNotEmpty() && max.isNotEmpty() -> "$min / $max"
        min.isNotEmpty() -> min
        max.isNotEmpty() -> max
        else -> temp
    }

private fun WanderCalendarDay.eventDescription(): String {
    val whenLabel = "$label $date"
    return listOf(ADD_EVENT, whenLabel, rangeLabel(), description)
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

private val DayWidth = 84.dp
private val EventDot = Color(0xFFF5C400)
private const val CARD_ALPHA = 0.38f
private const val CALENDAR = "Event calendar"
private const val ADD_EVENT = "Add event"
private const val SHIMMER_DAYS = 7
private const val SHIMMER_TRAVEL = 720f
private const val SHIMMER_SPAN = 220f
private const val SHIMMER_DROP = 48f
