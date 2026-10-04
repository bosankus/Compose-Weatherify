package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import bose.ankush.home.presentation.util.AirQualityIndexAnalyser
import bose.ankush.home.presentation.util.AirQualityIndexAnalyser.getFormattedAQI
import bose.ankush.home.presentation.util.toCelsius
import bose.ankush.home.presentation.util.toFormattedTime
import org.jetbrains.compose.resources.stringResource

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
            val status = AirQualityIndexAnalyser.getAQIAnalysedText(air.aqi).first
            val index = air.aqi.getFormattedAQI()
            WanderDetailCard(title = AIR_QUALITY, contentColor = contentColor) {
                Text(
                    text = status,
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = index,
                    color = contentColor,
                    fontSize = 13.sp,
                )
            }
        }
        if (hours.isNotEmpty()) {
            WanderDetailCard(
                title = stringResource(Res.string.hourly_forecast_heading_txt),
                contentColor = contentColor,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    hours.forEach { hour ->
                        HourCell(hour = hour, contentColor = contentColor)
                    }
                }
            }
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
    val title = alert.event?.takeIf { it.isNotBlank() } ?: alert.description.orEmpty()
    Text(
        text = title,
        color = contentColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    val body = alert.description?.takeIf { it.isNotBlank() && it != title }
    if (body != null) {
        Text(
            text = body,
            color = contentColor,
            fontSize = 13.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HourCell(
    hour: WeatherForecast.Hourly,
    contentColor: Color,
) {
    val time = hour.dt?.toFormattedTime().orEmpty()
    val temp = hour.temp?.let { "${it.toCelsius()}°" }.orEmpty()
    val spoken = listOf(time, temp).filter { it.isNotEmpty() }.joinToString(", ")
    Column(
        modifier = Modifier.semantics { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (time.isNotEmpty()) {
            Text(text = time, color = contentColor, fontSize = 13.sp)
        }
        if (temp.isNotEmpty()) {
            Text(
                text = temp,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private val cardFill = Color.Black.copy(alpha = 0.38f)
private const val HOURLY_LIMIT = 24
private const val ALERTS = "Weather alerts"
private const val AIR_QUALITY = "Air quality"
