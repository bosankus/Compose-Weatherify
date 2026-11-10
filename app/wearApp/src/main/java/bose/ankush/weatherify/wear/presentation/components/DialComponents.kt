package bose.ankush.weatherify.wear.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.wear.presentation.WeatherIconType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Centres this element on a point given in polar coordinates around the centre of its
 * parent (which must fill the screen). [angleDegrees] follows Canvas.drawArc: 0° is
 * 3 o'clock and angles grow clockwise, so -90° is 12 o'clock. [radiusFraction] is a
 * fraction of the dial's radius, so placement scales with any watch size.
 */
internal fun Modifier.polar(
    angleDegrees: Float,
    radiusFraction: Float,
): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val radius = min(width, height) / 2f * radiusFraction
        val radians = Math.toRadians(angleDegrees.toDouble())
        val x = width / 2f + radius * cos(radians).toFloat() - placeable.width / 2f
        val y = height / 2f + radius * sin(radians).toFloat() - placeable.height / 2f
        layout(width, height) { placeable.place(x.roundToInt(), y.roundToInt()) }
    }

/** True on the smallest round faces (~192dp), where dial text drops a size. */
@Composable
internal fun isSmallWatch(): Boolean = LocalConfiguration.current.screenWidthDp <= 195

/** Picks [small] on the smallest faces and [regular] elsewhere. */
@Composable
internal fun responsiveDp(
    small: Dp,
    regular: Dp,
): Dp = if (isSmallWatch()) small else regular

@Composable
internal fun horizontalScreenMargin(): Dp {
    val configuration = LocalConfiguration.current
    val fraction = if (configuration.isScreenRound) 0.1f else 0.05f
    return (configuration.screenWidthDp * fraction).dp
}

internal fun WeatherIconType.toImageVector(): ImageVector =
    when (this) {
        WeatherIconType.CLEAR -> Icons.Filled.WbSunny
        WeatherIconType.CLOUDS -> Icons.Filled.Cloud
        WeatherIconType.RAIN -> Icons.Filled.WaterDrop
        WeatherIconType.THUNDERSTORM -> Icons.Filled.Bolt
        WeatherIconType.SNOW -> Icons.Filled.AcUnit
        WeatherIconType.ATMOSPHERE -> Icons.Filled.Dehaze
        WeatherIconType.WIND -> Icons.Filled.Air
        WeatherIconType.UNKNOWN -> Icons.Filled.WbCloudy
    }
