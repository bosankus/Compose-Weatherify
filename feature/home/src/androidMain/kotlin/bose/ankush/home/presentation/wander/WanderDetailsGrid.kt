package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.home.domain.model.WeatherForecast

private data class DetailCell(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val secondary: String? = null,
)

/**
 * Feel, wind, UV, humidity, pressure, and clouds as a 3x2 grid drawn straight on the photo.
 * No fill: a soft text shadow and 1dp dividers keep it readable.
 */
@Composable
internal fun WanderDetailsGrid(
    content: WanderHomeContent,
    current: WeatherForecast.Current?,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val cells =
        listOf(
            DetailCell(Icons.Outlined.Thermostat, "Feel", content.feel),
            DetailCell(Icons.Outlined.Air, "Wind", content.wind, current?.wind_gust?.let { "Gust $it m/s" }),
            DetailCell(Icons.Outlined.WbSunny, "UV", content.uv),
            DetailCell(Icons.Outlined.WaterDrop, "Humidity", current?.humidity?.let { "$it%" } ?: NO_VALUE),
            DetailCell(Icons.Outlined.Speed, "Pressure", current?.pressure?.let { "$it hPa" } ?: NO_VALUE),
            DetailCell(Icons.Outlined.Cloud, "Clouds", current?.clouds?.let { "$it%" } ?: NO_VALUE),
        )
    val divider = contentColor.copy(alpha = DIVIDER_ALPHA)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = DETAILS,
            style = shadowed(contentColor, 13.sp, FontWeight.Medium),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        cells.chunked(COLUMNS).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(divider))
            }
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEachIndexed { index, cell ->
                    if (index > 0) {
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(divider))
                    }
                    DetailCellView(cell, contentColor, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailCellView(
    cell: DetailCell,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val spoken = listOfNotNull(cell.label, cell.value, cell.secondary).joinToString(", ")
    Column(
        modifier =
            modifier
                .semantics(mergeDescendants = true) { contentDescription = spoken }
                .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = cell.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp).padding(bottom = 2.dp),
        )
        Text(text = cell.value, style = shadowed(contentColor, 16.sp, FontWeight.Medium), maxLines = 1)
        cell.secondary?.let {
            Text(text = it, style = shadowed(contentColor.copy(alpha = MUTED_ALPHA), 11.sp), maxLines = 1)
        }
        Text(text = cell.label, style = shadowed(contentColor.copy(alpha = MUTED_ALPHA), 12.sp), maxLines = 1)
    }
}

private fun shadowed(
    color: Color,
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(color = color, fontSize = size, fontWeight = weight, shadow = TextShadow)

private val TextShadow = Shadow(color = Color.Black.copy(alpha = 0.4f), offset = Offset(0f, 1f), blurRadius = 6f)
private const val DETAILS = "Details"
private const val NO_VALUE = "—"
private const val COLUMNS = 3
private const val DIVIDER_ALPHA = 0.15f
private const val MUTED_ALPHA = 0.72f
