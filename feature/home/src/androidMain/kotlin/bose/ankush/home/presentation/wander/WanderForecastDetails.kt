package bose.ankush.home.presentation.wander

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.hourly_forecast_heading_txt
import bose.ankush.home.generated.resources.ic_sunny
import bose.ankush.home.presentation.util.formatTextCapitalization
import bose.ankush.home.presentation.util.getIconUrl
import bose.ankush.home.presentation.util.toCelsius
import coil3.compose.AsyncImage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/**
 * Alerts, air quality, and the hourly list already on [bose.ankush.home.presentation.HomeState].
 * Empty reports stay off the column. Nothing here is a placeholder.
 */
data class WanderForecastDetails(
    val alerts: List<WeatherForecast.Alert?> = emptyList(),
    val airQuality: AirQuality? = null,
    val hourly: List<WeatherForecast.Hourly?> = emptyList(),
)

@Composable
internal fun WanderForecastDetails(
    details: WanderForecastDetails,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onOpenAlert: (WeatherForecast.Alert) -> Unit = {},
) {
    val alerts = details.alerts.toWanderAlertRows()
    val air = details.airQuality?.takeIf { it.aqi > 0 }
    val hours =
        details.hourly
            .mapNotNull { it }
            .filter { it.dt != null }
            .take(HOURLY_LIMIT)
    if (alerts.isEmpty() && air == null && hours.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (alerts.isNotEmpty()) {
            WanderDetailCard(title = ALERTS) {
                alerts.forEach { row ->
                    AlertRow(
                        row = row,
                        contentColor = contentColor,
                        onOpen = { onOpenAlert(row.alert) },
                    )
                }
            }
        }
        if (air != null) {
            AirQualityCard(air = air, contentColor = contentColor)
        }
        if (hours.isNotEmpty()) {
            HourlyCard(hours = hours, contentColor = contentColor)
        }
    }
}

@Composable
private fun WanderDetailCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(cardFill, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .semantics { contentDescription = title },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun AlertRow(
    row: WanderAlertRowContent,
    contentColor: Color,
    onOpen: () -> Unit,
) {
    val title = row.title
    val issued = row.startText
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .semantics { contentDescription = "$title. Open alert details" }
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = AlertYellow,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!issued.isNullOrBlank()) {
                Text(text = issued, color = contentColor, fontSize = 13.sp, maxLines = 1)
            }
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.rotate(-90f),
        )
    }
}

@Composable
private fun HourlyCard(
    hours: List<WeatherForecast.Hourly>,
    contentColor: Color,
) {
    var selected by remember { mutableIntStateOf(0) }
    WanderDetailCard(title = stringResource(Res.string.hourly_forecast_heading_txt)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = HOUR_ZOOM_INSET_X, vertical = HOUR_ZOOM_INSET_Y),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            hours.forEachIndexed { index, hour ->
                HourCell(
                    hour = hour,
                    selected = selected == index,
                    contentColor = contentColor,
                    onSelect = { selected = index },
                )
            }
        }
    }
}

@Composable
private fun HourCell(
    hour: WeatherForecast.Hourly,
    selected: Boolean,
    contentColor: Color,
    onSelect: () -> Unit,
) {
    val time = hour.dt?.toClock().orEmpty()
    val temp = hour.temp?.let { "${it.toCelsius()}°" }.orEmpty()
    val weather = hour.weather?.firstOrNull()
    val description = weather?.description?.formatTextCapitalization().orEmpty()
    val spoken = listOf(time, temp, description).filter { it.isNotEmpty() }.joinToString(", ")
    val ink = contentColor
    val scale by animateFloatAsState(
        targetValue = if (selected) HOUR_SELECTED_SCALE else 1f,
        animationSpec = tween(durationMillis = HOUR_ZOOM_MILLIS, easing = FastOutSlowInEasing),
        label = "hourScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) HOUR_SELECTED_ELEVATION else 0.dp,
        animationSpec = tween(durationMillis = HOUR_ZOOM_MILLIS, easing = FastOutSlowInEasing),
        label = "hourElevation",
    )
    Column(
        modifier =
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = elevation.toPx()
                    shape = HourCellShape
                    clip = true
                    ambientShadowColor = HourShadow
                    spotShadowColor = HourShadow
                }.semantics {
                    contentDescription = spoken
                    this.selected = selected
                }.clickable(role = Role.Button, onClick = onSelect)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (time.isNotEmpty()) {
            Text(text = time, color = ink, fontSize = 13.sp)
        }
        val icon = weather?.icon?.takeIf { it.isNotBlank() }
        if (icon != null) {
            AsyncImage(
                model = icon.getIconUrl(),
                placeholder = painterResource(Res.drawable.ic_sunny),
                error = painterResource(Res.drawable.ic_sunny),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(28.dp),
            )
        }
        if (temp.isNotEmpty()) {
            Text(text = temp, color = ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        if (description.isNotEmpty()) {
            Text(
                text = description,
                color = ink,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun Long.toIssuedLabel(): String {
    val local = Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val month =
        local.month.name
            .take(MONTH_ABBREV)
            .lowercase()
            .replaceFirstChar { it.uppercaseChar() }
    return "$month ${local.day}, ${toClock()}"
}

private val cardFill = Color.Black.copy(alpha = 0.38f)
internal val AlertYellow = Color(0xFFF5C400)
private val HourCellShape = RoundedCornerShape(16.dp)
private val HourShadow = Color.Black.copy(alpha = 0.35f)
private val HOUR_SELECTED_ELEVATION = 3.dp
private val HOUR_ZOOM_INSET_X = 4.dp
private val HOUR_ZOOM_INSET_Y = 6.dp
private const val HOUR_SELECTED_SCALE = 1.07f
private const val HOUR_ZOOM_MILLIS = 220
private const val HOURLY_LIMIT = 24
private const val ALERTS = "Weather alerts"
private const val MONTH_ABBREV = 3
