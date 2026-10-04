package bose.ankush.home.presentation.wander

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import bose.ankush.home.presentation.util.AirQualityIndexAnalyser
import bose.ankush.home.presentation.util.AirQualityIndexAnalyser.getFormattedAQI
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
) {
    val alerts =
        details.alerts.mapNotNull { alert ->
            alert?.takeIf { !it.event.isNullOrBlank() || !it.description.isNullOrBlank() }
        }
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
            WanderDetailCard(title = ALERTS, contentColor = contentColor) {
                alerts.forEach { alert ->
                    AlertLine(alert = alert, contentColor = contentColor)
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
    contentColor: Color,
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
        Text(
            text = title,
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        content()
    }
}

@Composable
private fun AlertLine(
    alert: WeatherForecast.Alert,
    contentColor: Color,
) {
    var expanded by remember(alert.event, alert.start, alert.end) { mutableStateOf(false) }
    val title = alert.event?.takeIf { it.isNotBlank() } ?: alert.description.orEmpty()
    val body = alert.description?.takeIf { it.isNotBlank() }
    val issued = alert.start?.toIssuedLabel()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .semantics {
                    contentDescription = if (expanded) "Collapse alert" else "Expand alert"
                }.clickable(role = Role.Button) { expanded = !expanded }
                .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Weather alert",
                tint = AlertYellow,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!issued.isNullOrBlank()) {
                    Text(text = issued, color = contentColor, fontSize = 13.sp)
                }
            }
        }
        if (!body.isNullOrBlank() && body != title) {
            Text(
                text = body,
                color = contentColor,
                fontSize = 13.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (expanded) {
            Text(text = "Source", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = alert.sender_name?.takeIf { it.isNotBlank() } ?: "Unknown",
                color = contentColor,
                fontSize = 13.sp,
            )
            Text(text = "Valid until", color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = alert.end?.toIssuedLabel() ?: "Unknown", color = contentColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AirQualityCard(
    air: AirQuality,
    contentColor: Color,
) {
    var expanded by remember { mutableStateOf(false) }
    val status = AirQualityIndexAnalyser.getAQIAnalysedText(air.aqi).first
    val index = air.aqi.getFormattedAQI()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardFill)
                .semantics { contentDescription = if (expanded) "Collapse air quality" else "Expand air quality" }
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = AIR_QUALITY,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }
        Text(text = status, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(text = index, color = contentColor, fontSize = 13.sp)
        if (expanded) {
            PollutantGrid(air = air, contentColor = contentColor)
            Text(text = UNIT_NOTE, color = contentColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PollutantGrid(
    air: AirQuality,
    contentColor: Color,
) {
    val rows =
        listOf(
            listOf("PM2.5" to air.pm25, "PM10" to air.pm10),
            listOf("CO" to air.co, "O3" to air.o3),
            listOf("NO2" to air.no2, "SO2" to air.so2),
        )
    rows.forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { (name, value) ->
                Text(
                    text = "$name ${value.toInt()}",
                    color = contentColor,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HourlyCard(
    hours: List<WeatherForecast.Hourly>,
    contentColor: Color,
) {
    var selected by remember { mutableIntStateOf(0) }
    WanderDetailCard(
        title = stringResource(Res.string.hourly_forecast_heading_txt),
        contentColor = contentColor,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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
    val ink = if (selected) WanderOnLight else contentColor
    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) Color.White else Color.Transparent)
                .semantics { contentDescription = spoken }
                .clickable(role = Role.Button, onClick = onSelect)
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

private fun Long.toIssuedLabel(): String {
    val local = Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val month =
        local.month.name
            .take(MONTH_ABBREV)
            .lowercase()
            .replaceFirstChar { it.uppercaseChar() }
    return "$month ${local.day}, ${toClock()}"
}

private val cardFill = Color.Black.copy(alpha = 0.38f)
private val AlertYellow = Color(0xFFF5C400)
private const val HOURLY_LIMIT = 24
private const val ALERTS = "Weather alerts"
private const val AIR_QUALITY = "Air quality"
private const val UNIT_NOTE = "Concentration in μg/m³"
private const val MONTH_ABBREV = 3
