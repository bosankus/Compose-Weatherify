package bose.ankush.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Umbrella
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
 * Rain today and next rain append as an extra row only when their values are present, padded to
 * three slots so its dividers line up with the rows above.
 * No fill: a soft text shadow and 1dp dividers keep it readable.
 */
@Composable
internal fun WeatherDetailsGrid(
    content: HomeWeatherContent,
    current: WeatherForecast.Current?,
    contentColor: Color,
    modifier: Modifier = Modifier,
    extras: ForecastExtras = ForecastExtras(),
) {
    val cells =
        buildList {
            add(DetailCell(Icons.Outlined.Thermostat, "Feel", content.feel))
            add(
                DetailCell(
                    Icons.Outlined.Air,
                    "Wind",
                    content.wind,
                    current?.wind_gust?.let {
                        "Gust ${
                            formatAnimatedNumber(
                                it,
                                decimals = if (it % 1.0 == 0.0) 0 else 1,
                                suffix = " m/s"
                            )
                        }"
                    },
                ),
            )
            add(DetailCell(Icons.Outlined.WbSunny, "UV", content.uv))
            add(
                DetailCell(
                    Icons.Outlined.WaterDrop,
                    "Humidity",
                    formatAnimatedNumber(current?.humidity?.toDouble(), suffix = "%"),
                ),
            )
            add(
                DetailCell(
                    Icons.Outlined.Speed,
                    "Pressure",
                    formatAnimatedNumber(current?.pressure?.toDouble(), suffix = " hPa"),
                ),
            )
            add(
                DetailCell(
                    Icons.Outlined.Cloud,
                    "Clouds",
                    formatAnimatedNumber(current?.clouds?.toDouble(), suffix = "%"),
                ),
            )
            extras.rainToday?.let { add(DetailCell(Icons.Outlined.Umbrella, "Rain today", it)) }
            extras.nextRain?.let { add(DetailCell(Icons.Outlined.Schedule, "Next rain", it)) }
        }
    val divider = contentColor.copy(alpha = DIVIDER_ALPHA)
    Column(modifier = modifier.fillMaxWidth()) {
        detailGridRows(cells, COLUMNS).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(divider))
            }
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEachIndexed { index, cell ->
                    if (index > 0) {
                        // Empty slots keep a transparent 1dp gap so the weighted columns match the rows above.
                        val gap = if (cell != null) Modifier.background(divider) else Modifier
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().then(gap))
                    }
                    if (cell != null) {
                        DetailCellView(cell, contentColor, Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Splits [cells] into rows of exactly [columns] slots, padding the last row with nulls so a short
 * row (e.g. only Rain today) still lines up with the full rows above it.
 */
internal fun <T : Any> detailGridRows(
    cells: List<T>,
    columns: Int,
): List<List<T?>> = cells.chunked(columns).map { row -> row + List(columns - row.size) { null } }

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
        AnimatedValueText(
            text = cell.value,
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            style = shadowed(contentColor, 16.sp, FontWeight.Medium),
        )
        cell.secondary?.let {
            AnimatedValueText(
                text = it,
                color = contentColor.copy(alpha = MUTED_ALPHA),
                fontSize = 11.sp,
                style = shadowed(contentColor.copy(alpha = MUTED_ALPHA), 11.sp),
            )
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
private const val COLUMNS = 3
private const val DIVIDER_ALPHA = 0.15f
private const val MUTED_ALPHA = 0.72f
