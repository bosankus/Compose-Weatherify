package bose.ankush.weatherify.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import bose.ankush.weatherify.wear.presentation.WeatherUiState
import bose.ankush.weatherify.wear.presentation.WeatherOverlay
import bose.ankush.weatherify.wear.presentation.components.responsiveDp
import bose.ankush.weatherify.wear.presentation.components.toImageVector
import bose.ankush.weatherify.wear.presentation.theme.DialColors
import bose.ankush.weatherify.wear.presentation.toLottieRes

private data class TodayFact(
    val label: String,
    val value: String?,
    val icon: ImageVector,
)

/**
 * Today's secondary facts as a grid of round tiles. Tap a tile to read its value on the
 * line underneath; tiles with no synced data are dimmed.
 */
@Composable
internal fun TodayGridScreen(uiState: WeatherUiState) {
    val today = uiState.today
    val facts =
        listOf(
            TodayFact("Sunrise", today.sunrise, Icons.Filled.WbTwilight),
            TodayFact("Sunset", today.sunset, Icons.Filled.NightsStay),
            TodayFact("Air", today.airQuality, Icons.Filled.Masks),
            TodayFact("Pressure", today.pressure, Icons.Filled.Speed),
            TodayFact("Rain", today.rain, Icons.Filled.Umbrella),
            TodayFact("Clouds", today.clouds, Icons.Filled.Cloud),
        )
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tileSize = responsiveDp(small = 40.dp, regular = 46.dp)
    val gap = responsiveDp(small = 8.dp, regular = 10.dp)

    ScreenScaffold {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val lottie = uiState.icon.toLottieRes(uiState.dayPhase)
                if (lottie != null) {
                    WeatherOverlay(res = lottie, modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        imageVector = uiState.icon.toImageVector(),
                        contentDescription = null,
                        tint = DialColors.TextPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "TODAY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = DialColors.Crimson,
                )
            }
            Spacer(Modifier.height(gap))
            facts.chunked(3).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(Modifier.height(gap))
                Row(horizontalArrangement = Arrangement.spacedBy(gap + 4.dp)) {
                    row.forEachIndexed { columnIndex, fact ->
                        val index = rowIndex * 3 + columnIndex
                        FactTile(
                            fact = fact,
                            isSelected = index == selected,
                            size = tileSize,
                            onClick = { selected = index },
                        )
                    }
                }
            }
            Spacer(Modifier.height(gap))
            val fact = facts[selected]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = fact.icon,
                    contentDescription = null,
                    tint = DialColors.TextMuted,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${fact.label}  ${fact.value ?: "—"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DialColors.TextPrimary.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FactTile(
    fact: TodayFact,
    isSelected: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val background: Brush = if (isSelected) DialColors.AccentGradient else SolidColor(DialColors.Tile)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(background)
                .clickable(role = Role.Tab, onClick = onClick)
                .semantics {
                    selected = isSelected
                    contentDescription = "${fact.label} ${fact.value ?: "unavailable"}"
                },
    ) {
        Icon(
            imageVector = fact.icon,
            contentDescription = null,
            tint = DialColors.TextPrimary.copy(alpha = if (fact.value == null) 0.35f else 1f),
            modifier = Modifier.size(size * 0.45f),
        )
    }
}
