package bose.ankush.home.presentation.wander

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
import bose.ankush.home.presentation.shell.ShellSky
import bose.ankush.home.presentation.shell.shellSky
import bose.ankush.home.presentation.util.formatTextCapitalization
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource

/**
 * Condition icon and description, then the sunrise and sunset arc.
 * The Unsplash photo stays behind this card.
 */
@Composable
internal fun WanderCurrentReport(
    current: WeatherForecast.Current,
    contentColor: Color,
    modifier: Modifier = Modifier,
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
        WanderDayArc(
            sunrise = current.sunrise,
            sunset = current.sunset,
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
            imageVector = wanderSkyIcon(current),
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
 * Vector icon from the same sky mapping the shell home uses, so it always renders in the
 * Wander ink. Clear at night (OpenWeather icon code ending in "n") shows a moon.
 */
private fun wanderSkyIcon(current: WeatherForecast.Current): ImageVector {
    val night =
        current.weather
            ?.firstOrNull()
            ?.icon
            ?.endsWith("n") == true
    return when (shellSky(current)) {
        ShellSky.Clear -> if (night) Icons.Outlined.NightsStay else Icons.Outlined.WbSunny
        ShellSky.Clouds, null -> Icons.Outlined.Cloud
        ShellSky.Rain -> Icons.Outlined.WaterDrop
        ShellSky.Snow -> Icons.Outlined.AcUnit
        ShellSky.Thunderstorm -> Icons.Outlined.Thunderstorm
        ShellSky.Mist -> Icons.Outlined.Dehaze
    }
}

/** Device-zone clock, as before. Wander sun times use [toClock] with [wanderForecastZone]. */
internal fun Long.toClock(): String = toClock(TimeZone.currentSystemDefault())

private val cardFill = Color.Black.copy(alpha = 0.38f)
private const val CURRENT_WEATHER = "Current weather"
