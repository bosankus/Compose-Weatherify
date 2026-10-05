package bose.ankush.home.presentation.wander

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.presentation.util.AirQualityIndexAnalyser

/**
 * Air quality on the Wander column. Collapsed: level dot, status, AQI, and the dominant
 * pollutant. Tap expands a 3x2 grid of pollutant tiles with level bars.
 */
@Composable
internal fun AirQualityCard(
    air: AirQuality,
    contentColor: Color,
) {
    var expanded by remember { mutableStateOf(false) }
    val status = AirQualityIndexAnalyser.getAQIAnalysedText(air.aqi).first
    val readings = remember(air) { air.pollutantReadings() }
    val dominant = remember(readings) { readings.dominant() }
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(EXPAND_MILLIS),
        label = "airQualityChevron",
    )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AirCardFill)
                .semantics {
                    contentDescription =
                        if (expanded) "Collapse air quality" else "Expand air quality"
                }.clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(10.dp).background(airLevelFor(air.aqi).color, CircleShape))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = status,
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = "AQI ${formatAqi(air.aqi)}", color = contentColor.copy(alpha = MUTED), fontSize = 13.sp)
            }
            dominant?.let { DominantChip(it.pollutant, contentColor) }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.graphicsLayer { rotationZ = chevron },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(EXPAND_MILLIS)) + fadeIn(tween(EXPAND_MILLIS)),
            exit = shrinkVertically(tween(EXPAND_MILLIS)) + fadeOut(tween(EXPAND_MILLIS)),
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                readings.chunked(GRID_COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { reading ->
                            PollutantTile(reading, contentColor, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DominantChip(
    pollutant: Pollutant,
    contentColor: Color,
) {
    Row(
        modifier =
            Modifier
                .background(TileFill, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = pollutant.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp),
        )
        Text(text = pollutant.label, color = contentColor, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun PollutantTile(
    reading: PollutantReading,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val name = reading.pollutant.label
    val value = formatConcentration(reading.value)
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(TileFill)
                .semantics(mergeDescendants = true) { contentDescription = "$name $value $UNIT" }
                .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = reading.pollutant.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(text = name, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                text = " $UNIT",
                color = contentColor.copy(alpha = MUTED),
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(contentColor.copy(alpha = TRACK_ALPHA)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(reading.fraction)
                        .height(3.dp)
                        .background(reading.level.color, RoundedCornerShape(2.dp)),
            )
        }
    }
}

private val AirLevel.color: Color
    get() =
        when (this) {
            AirLevel.GOOD -> Color(0xFF4CAF50)
            AirLevel.FAIR -> Color(0xFF9CCC65)
            AirLevel.MODERATE -> Color(0xFFFFB300)
            AirLevel.POOR -> Color(0xFFFB8C00)
            AirLevel.VERY_POOR -> Color(0xFFE53935)
        }

private val Pollutant.icon: ImageVector
    get() =
        when (this) {
            Pollutant.PM25 -> Icons.Outlined.Grain
            Pollutant.PM10 -> Icons.Outlined.BlurOn
            Pollutant.CO -> Icons.Outlined.LocalFireDepartment
            Pollutant.O3 -> Icons.Outlined.WbSunny
            Pollutant.NO2 -> Icons.Outlined.DirectionsCar
            Pollutant.SO2 -> Icons.Outlined.Factory
        }

private val AirCardFill = Color.Black.copy(alpha = 0.38f)
private val TileFill = Color.White.copy(alpha = 0.08f)
private const val UNIT = "µg/m³"
private const val MUTED = 0.7f
private const val TRACK_ALPHA = 0.15f
private const val EXPAND_MILLIS = 220
private const val GRID_COLUMNS = 3
