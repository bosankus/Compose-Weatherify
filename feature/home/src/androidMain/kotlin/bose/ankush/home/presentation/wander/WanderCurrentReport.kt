package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.ic_sunny
import bose.ankush.home.generated.resources.weather_icon_content
import bose.ankush.home.presentation.component.SunriseSunsetCombinedAnimation
import bose.ankush.home.presentation.util.formatTextCapitalization
import bose.ankush.home.presentation.util.getFormattedDateTimeFromEpoch
import bose.ankush.home.presentation.util.getIconUrl
import coil3.compose.AsyncImage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Observation, icon, and today's summary on the Wander column.
 * The sunrise drawing sits in this card. The Unsplash photo stays behind it.
 */
@Composable
internal fun WanderCurrentReport(
    current: WeatherForecast.Current,
    todaySummary: String?,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val weather = current.weather?.firstOrNull()
    val description = weather?.description?.formatTextCapitalization().orEmpty()
    val observed = current.dt?.let(::observedLabel)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(cardFill, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .semantics { contentDescription = CURRENT_WEATHER },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!observed.isNullOrBlank()) {
            Text(text = observed, color = contentColor, fontSize = 13.sp)
        }
        ConditionRow(icon = weather?.icon, description = description, contentColor = contentColor)
        DetailLine(text = metricLine(current), contentColor = contentColor)
        SunTimes(current = current, contentColor = contentColor)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(SunBoxHeight)
                    .clip(RoundedCornerShape(16.dp)),
        ) {
            SunriseSunsetCombinedAnimation(
                sunriseTimestamp = current.sunrise,
                sunsetTimestamp = current.sunset,
                currentTimestamp = Clock.System.now().epochSeconds,
            )
        }
        val summary = todaySummary?.takeIf { it.isNotBlank() }
        if (summary != null) {
            Text(
                text = TODAY,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = summary,
                color = contentColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun ConditionRow(
    icon: String?,
    description: String,
    contentColor: Color,
) {
    if (icon.isNullOrBlank() && description.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!icon.isNullOrBlank()) {
            AsyncImage(
                model = icon.getIconUrl(),
                placeholder = painterResource(Res.drawable.ic_sunny),
                error = painterResource(Res.drawable.ic_sunny),
                contentDescription = stringResource(Res.string.weather_icon_content),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp),
            )
        }
        if (description.isNotBlank()) {
            Text(
                text = description,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DetailLine(
    text: String,
    contentColor: Color,
) {
    if (text.isBlank()) return
    Text(text = text, color = contentColor, fontSize = 14.sp)
}

@Composable
private fun SunTimes(
    current: WeatherForecast.Current,
    contentColor: Color,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = "Sunrise ${current.sunrise?.toClock() ?: UNAVAILABLE}",
            color = contentColor,
            fontSize = 14.sp,
        )
        Text(
            text = "Sunset ${current.sunset?.toClock() ?: UNAVAILABLE}",
            color = contentColor,
            fontSize = 14.sp,
        )
    }
}

private fun metricLine(current: WeatherForecast.Current): String =
    listOfNotNull(
        current.humidity?.let { "Humidity $it%" },
        current.pressure?.let { "Pressure $it hPa" },
        current.clouds?.let { "Clouds $it%" },
        current.wind_gust?.let { "Gust $it m/s" },
    ).joinToString("   ")

private fun observedLabel(epochSeconds: Long): String {
    val date = getFormattedDateTimeFromEpoch(epochSeconds)
    return "$date, ${epochSeconds.toClock()}"
}

internal fun Long.toClock(): String {
    val local = Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 =
        when {
            local.hour == 0 -> 12
            local.hour > 12 -> local.hour - 12
            else -> local.hour
        }
    val minute = local.minute.toString().padStart(2, '0')
    val amPm = if (local.hour < 12) "AM" else "PM"
    return "$hour12:$minute $amPm"
}

private val cardFill = Color.Black.copy(alpha = 0.38f)
private val SunBoxHeight = 112.dp
private const val CURRENT_WEATHER = "Current weather"
private const val TODAY = "Today's forecast"
private const val UNAVAILABLE = "N/A"
