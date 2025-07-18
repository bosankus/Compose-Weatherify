package bose.ankush.weatherify.presentation.home.component

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.base.common.AirQualityIndexAnalyser.getAQIAnalysedText
import bose.ankush.weatherify.base.common.AirQualityIndexAnalyser.getFormattedAQI
import bose.ankush.weatherify.domain.model.AirQuality

/**
 * This composable is response to show air quality card on HomeScreen.
 * Shows what is the current air quality based return value of [getAQIAnalysedText]
 */
@SuppressLint("MissingPermission")
@Composable
internal fun BriefAirQualityReportCardLayout(airQuality: AirQuality) {
    ShowUI(aq = airQuality)
}

@Composable
private fun ShowUI(aq: AirQuality) {
    // Pre-calculate values that don't change during composition
    // Use remember to cache these values based on aq.aqi
    val (fullStatusText, _) = remember(aq.aqi) { getAQIAnalysedText(aq.aqi) }
    val qualityColor = remember(aq.aqi) { getAirQualityColor(aq.aqi) }
    val statusText = remember(fullStatusText) { fullStatusText.split(" at")[0] }
    val qualityColorAlpha = remember(qualityColor) { qualityColor.copy(alpha = 0.2f) }

    // Pre-calculate pollutant values
    val pm25Value = remember(aq.pm25) { "${aq.pm25.toInt()} μg/m³" }
    val coValue = remember(aq.co) { "${aq.co.toInt()} μg/m³" }
    val o3Value = remember(aq.o3) { "${aq.o3.toInt()} μg/m³" }

    // Pre-calculate formatted AQI
    val formattedAQI = remember(aq.aqi) { aq.aqi.getFormattedAQI() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Air Quality Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(qualityColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Air Quality",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AQI Value and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedAQI,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "AQI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    color = qualityColorAlpha,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = qualityColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PollutantItem(name = "PM2.5", value = pm25Value)
                PollutantItem(name = "CO", value = coValue)
                PollutantItem(name = "O₃", value = o3Value)
            }
        }
    }
}

/**
 * Returns a color based on the air quality index value
 * Not a composable function since it doesn't use any composable functions
 */
private fun getAirQualityColor(aqi: Int): Color {
    return when (aqi) {
        in 0..50 -> Color(0xFF4CAF50)      // Good - Green
        in 51..100 -> Color(0xFFFFEB3B)     // Moderate - Yellow
        in 101..150 -> Color(0xFFFF9800)    // Unhealthy for sensitive groups - Orange
        in 151..200 -> Color(0xFFE53935)    // Unhealthy - Red
        in 201..300 -> Color(0xFF9C27B0)    // Very Unhealthy - Purple
        else -> Color(0xFF7E0023)           // Hazardous - Dark Red
    }
}

/**
 * Displays a single pollutant item with name and value
 * Optimized to use Box instead of Surface for better performance
 */
@Composable
private fun PollutantItem(name: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Use Box instead of Surface for better performance
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
