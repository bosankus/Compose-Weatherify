package bose.ankush.home.presentation.home.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.presentation.home.util.AirQualityIndexAnalyser.getAQIAnalysedText
import bose.ankush.home.presentation.home.util.AirQualityIndexAnalyser.getFormattedAQI

// OWM AQI scale (1–6) to EPA AQI scale (0–500) mapping
private const val OWM_AQI_GOOD = 1
private const val OWM_AQI_FAIR = 2
private const val OWM_AQI_MODERATE = 3
private const val OWM_AQI_POOR = 4
private const val OWM_AQI_VERY_POOR = 5
private const val OWM_AQI_EXTREME = 6
private const val EPA_AQI_GOOD_MID = 25
private const val EPA_AQI_MODERATE_MID = 75
private const val EPA_AQI_UNHEALTHY_SENSITIVE_MID = 125
private const val EPA_AQI_UNHEALTHY_MID = 175
private const val EPA_AQI_VERY_UNHEALTHY_MID = 250
private const val EPA_AQI_HAZARDOUS_MID = 425
private const val EPA_AQI_MIN = 0
private const val EPA_AQI_MAX = 500

// EPA AQI color range thresholds
private const val EPA_GOOD_MAX = 50
private const val EPA_MODERATE_MAX = 100
private const val EPA_UNHEALTHY_SENSITIVE_MAX = 150
private const val EPA_UNHEALTHY_MAX = 200
private const val EPA_VERY_UNHEALTHY_MAX = 300

// AQI colors (ARGB hex)
private const val COLOR_AQI_GOOD = 0xFF4CAF50L
private const val COLOR_AQI_MODERATE = 0xFFFFEB3BL
private const val COLOR_AQI_UNHEALTHY_SENSITIVE = 0xFFFF9800L
private const val COLOR_AQI_UNHEALTHY = 0xFFE53935L
private const val COLOR_AQI_VERY_UNHEALTHY = 0xFF9C27B0L
private const val COLOR_AQI_HAZARDOUS = 0xFF7E0023L

private data class AqiUiState(
    val statusText: String,
    val qualityColor: Color,
    val formattedAqi: String,
)

@Composable
private fun rememberAqiUiState(aqi: Int): AqiUiState =
    remember(aqi) {
        val (fullStatusText, _) = getAQIAnalysedText(aqi)
        // Convert OpenWeatherMap 1-6 scale to EPA 0-500 scale for color mapping
        val epaAqi = convertOwmAqiToEpa(aqi)
        AqiUiState(
            statusText = fullStatusText.split(" at").firstOrNull() ?: "",
            qualityColor = getAirQualityColor(epaAqi),
            formattedAqi = aqi.getFormattedAQI(),
        )
    }

@Composable
internal fun BriefAirQualityReportCardLayout(airQuality: AirQuality) {
    val aqiUiState = rememberAqiUiState(airQuality.aqi)
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = { isExpanded = !isExpanded },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            AqiSummary(aqiUiState = aqiUiState, isExpanded = isExpanded)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isExpanded) {
                ExpandedPollutantsDetails(airQuality = airQuality)
            } else {
                KeyPollutants(airQuality = airQuality)
            }
        }
    }
}

@Composable
private fun AqiSummary(
    aqiUiState: AqiUiState,
    isExpanded: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .background(aqiUiState.qualityColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = aqiUiState.formattedAqi,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColorFor(backgroundColor = aqiUiState.qualityColor),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Air Quality",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = aqiUiState.statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            modifier = Modifier.rotate(if (isExpanded) 180f else 0f),
        )
    }
}

@Composable
private fun KeyPollutants(airQuality: AirQuality) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            PollutantItem(name = "PM2.5", value = airQuality.pm25.toInt().toString())
            PollutantItem(name = "CO", value = airQuality.co.toInt().toString())
            PollutantItem(name = "O₃", value = airQuality.o3.toInt().toString())
        }
    }
}

@Composable
private fun ExpandedPollutantsDetails(airQuality: AirQuality) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PollutantItem(
                modifier = Modifier.weight(1f),
                name = "CO",
                value = airQuality.co.toInt().toString(),
            )
            PollutantItem(
                modifier = Modifier.weight(1f),
                name = "NO₂",
                value = airQuality.no2.toInt().toString(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PollutantItem(
                modifier = Modifier.weight(1f),
                name = "O₃",
                value = airQuality.o3.toInt().toString(),
            )
            PollutantItem(
                modifier = Modifier.weight(1f),
                name = "SO₂",
                value = airQuality.so2.toInt().toString(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PollutantItem(
                modifier = Modifier.weight(1f),
                name = "PM10",
                value = airQuality.pm10.toInt().toString(),
            )
            PollutantItem(
                modifier = Modifier.weight(1f),
                name = "PM2.5",
                value = airQuality.pm25.toInt().toString(),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Concentration in μg/m³",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Converts OpenWeatherMap AQI scale (1-6) to EPA AQI scale (0-500)
 *
 * OWM Scale: 1 Good, 2 Fair, 3 Moderate, 4 Poor, 5 Very Poor, 6 Extreme.
 * EPA Scale: 0-50 Good, 51-100 Moderate, 101-150 Unhealthy for Sensitive Groups,
 * 151-200 Unhealthy, 201-300 Very Unhealthy, 301+ Hazardous.
 */
private fun convertOwmAqiToEpa(owmAqi: Int): Int =
    when (owmAqi) {
        OWM_AQI_GOOD -> EPA_AQI_GOOD_MID
        OWM_AQI_FAIR -> EPA_AQI_MODERATE_MID
        OWM_AQI_MODERATE -> EPA_AQI_UNHEALTHY_SENSITIVE_MID
        OWM_AQI_POOR -> EPA_AQI_UNHEALTHY_MID
        OWM_AQI_VERY_POOR -> EPA_AQI_VERY_UNHEALTHY_MID
        OWM_AQI_EXTREME -> EPA_AQI_HAZARDOUS_MID
        else -> owmAqi.coerceIn(EPA_AQI_MIN, EPA_AQI_MAX)
    }

private fun getAirQualityColor(aqi: Int): Color =
    when (aqi) {
        in EPA_AQI_MIN..EPA_GOOD_MAX -> Color(COLOR_AQI_GOOD)
        in (EPA_GOOD_MAX + 1)..EPA_MODERATE_MAX -> Color(COLOR_AQI_MODERATE)
        in (EPA_MODERATE_MAX + 1)..EPA_UNHEALTHY_SENSITIVE_MAX ->
            Color(
                COLOR_AQI_UNHEALTHY_SENSITIVE,
            )

        in (EPA_UNHEALTHY_SENSITIVE_MAX + 1)..EPA_UNHEALTHY_MAX -> Color(COLOR_AQI_UNHEALTHY)
        in (EPA_UNHEALTHY_MAX + 1)..EPA_VERY_UNHEALTHY_MAX -> Color(COLOR_AQI_VERY_UNHEALTHY)
        else -> Color(COLOR_AQI_HAZARDOUS)
    }

@Composable
private fun PollutantItem(
    name: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
