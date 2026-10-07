package bose.ankush.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.domain.model.WeatherForecast

/**
 * Alert details in a [SheetPanel]. The caller bounds it above the tab bar, so the
 * tabs stay tappable and the system bars and photo never change. Long content opens at a
 * peek height and drags or scrolls up to the full area.
 */
@Composable
internal fun WeatherAlertPanel(
    alert: WeatherForecast.Alert?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Plain holder, not state: keeps the last alert on screen while the panel slides out
    // without a write during composition.
    val lastAlert = remember { arrayOfNulls<WeatherForecast.Alert>(1) }
    if (alert != null) lastAlert[0] = alert
    val shown = alert ?: lastAlert[0]
    SheetPanel(
        visible = alert != null,
        spec = SheetPanelSpec(onDismiss = onDismiss, closeLabel = CLOSE_ALERT, key = shown),
        modifier = modifier,
        header = { shown?.let { AlertHeader(alert = it, onDismiss = onDismiss) } },
        body = { shown?.let { AlertBody(alert = it) } },
    )
}

@Composable
private fun AlertHeader(
    alert: WeatherForecast.Alert,
    onDismiss: () -> Unit,
) {
    val title = alert.event?.takeIf { it.isNotBlank() } ?: ALERT_ROW_FALLBACK_TITLE
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = AlertYellow,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            color = ContentOnDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = CLOSE_ALERT,
                tint = ContentOnDark,
            )
        }
    }
}

@Composable
private fun AlertBody(alert: WeatherForecast.Alert) {
    val parsed = remember(alert.description) { parseAlertDescription(alert.description) }
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
        parsed.fallback?.let { Text(text = it, color = ContentOnDark, fontSize = 14.sp) }
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
                color = ContentOnDark,
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
        color = ContentOnDark,
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
            Text(text = section.body, color = ContentOnDark, fontSize = 14.sp)
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
                color = ContentOnDark,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    if (items.size > AREA_PREVIEW) {
        Text(
            text = if (showAll) "Show less" else "Show all (${items.size})",
            color = ContentOnDark,
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

private val Muted = SheetMutedText
private val ChipFill = ContentOnDark.copy(alpha = 0.12f)
private const val AREA_PREVIEW = 4
private const val CLOSE_ALERT = "Close alert details"
