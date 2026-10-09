package bose.ankush.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.weather_icon_content
import bose.ankush.home.presentation.util.formatTextCapitalization
import org.jetbrains.compose.resources.stringResource

/**
 * Condition icon and description, then the sunrise and sunset arc.
 * The Unsplash photo stays behind this card.
 */
@Composable
internal fun CurrentWeatherReport(
    current: WeatherForecast.Current,
    contentColor: Color,
    modifier: Modifier = Modifier,
    timezoneOffset: Int? = null,
) {
    val weather = current.weather?.firstOrNull()
    val description = weather?.description?.formatTextCapitalization().orEmpty()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(cardFill, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .semantics { contentDescription = CURRENT_WEATHER },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ConditionRow(current = current, description = description, contentColor = contentColor)
        DaylightArc(
            sunrise = current.sunrise,
            sunset = current.sunset,
            timezoneOffset = timezoneOffset,
            contentColor = contentColor,
        )
    }
}

@Composable
private fun ConditionRow(
    current: WeatherForecast.Current,
    description: String,
    contentColor: Color,
) {
    if (description.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = currentSkyIcon(current),
            contentDescription = stringResource(Res.string.weather_icon_content),
            tint = contentColor,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = description,
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Vector icon for the current sky, so it always renders in the home screen ink.
 * Clear at night (OpenWeather icon code ending in "n") shows a moon.
 */
private fun currentSkyIcon(current: WeatherForecast.Current): ImageVector {
    val night =
        current.weather
            ?.firstOrNull()
            ?.icon
            ?.endsWith("n") == true
    return when (skyKind(current)) {
        SkyKind.Clear -> if (night) Icons.Outlined.NightsStay else Icons.Outlined.WbSunny
        SkyKind.Clouds, null -> Icons.Outlined.Cloud
        SkyKind.Rain -> Icons.Outlined.WaterDrop
        SkyKind.Snow -> Icons.Outlined.AcUnit
        SkyKind.Thunderstorm -> Icons.Outlined.Thunderstorm
        SkyKind.Mist -> Icons.Outlined.Dehaze
    }
}

private val cardFill = Color.Black.copy(alpha = 0.38f)
private const val CURRENT_WEATHER = "Current weather"

/** Coarse sky used only to pick an icon. Null when the report names nothing we can map. */
private enum class SkyKind {
    Clear,
    Clouds,
    Rain,
    Snow,
    Thunderstorm,
    Mist,
}

/** Thunder before rain, snow before rain, so mixed phrases keep the stronger sky. */
private fun skyKind(now: WeatherForecast.Current): SkyKind? {
    val weather = now.weather?.firstOrNull() ?: return null
    val text = "${weather.main} ${weather.description}".lowercase()
    return when {
        text.isBlank() -> null
        "thunder" in text -> SkyKind.Thunderstorm
        "snow" in text || "sleet" in text -> SkyKind.Snow
        "drizzle" in text || "rain" in text -> SkyKind.Rain
        "clear" in text -> SkyKind.Clear
        "cloud" in text -> SkyKind.Clouds
        MIST_WORDS.any { it in text } -> SkyKind.Mist
        else -> null
    }
}

private val MIST_WORDS = listOf("mist", "fog", "haze", "smoke", "dust", "sand", "ash")
