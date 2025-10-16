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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bose.ankush.weatherify.base.common.AirQualityIndexAnalyser.getAQIAnalysedText
import bose.ankush.weatherify.base.common.AirQualityIndexAnalyser.getFormattedAQI
import bose.ankush.weatherify.domain.model.AirQuality

private data class AqiUiState(
    val statusText: String,
    val qualityColor: Color,
    val formattedAqi: String,
)

@Composable
private fun rememberAqiUiState(aqi: Int): AqiUiState {
    return remember(aqi) {
        val (fullStatusText, _) = getAQIAnalysedText(aqi)
        AqiUiState(
            statusText = fullStatusText.split(" at").firstOrNull() ?: "",
            qualityColor = getAirQualityColor(aqi),
            formattedAqi = aqi.getFormattedAQI()
        )
    }
}

/**
 * This composable is response to show air quality card on HomeScreen.
 * Shows what is the current air quality based return value of [getAQIAnalysedText]
 */
@SuppressLint("MissingPermission")
@Composable
internal fun BriefAirQualityReportCardLayout(airQuality: AirQuality) {
    val aqiUiState = rememberAqiUiState(airQuality.aqi)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp), // Increased corner radius for a softer look
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Lower elevation
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            AqiSummary(aqiUiState = aqiUiState)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            PollutantsDetails(airQuality = airQuality)
        }
    }
}

@Composable
private fun AqiSummary(aqiUiState: AqiUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp) // Slightly smaller
                .background(aqiUiState.qualityColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = aqiUiState.formattedAqi,
                style = MaterialTheme.typography.headlineMedium, // Adjusted style for size
                fontWeight = FontWeight.Bold,
                color = contentColorFor(backgroundColor = aqiUiState.qualityColor)
            )
        }

        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Air Quality",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = aqiUiState.statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold, // Slightly less bold
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PollutantsDetails(airQuality: AirQuality) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            PollutantItem(name = "PM2.5", value = airQuality.pm25.toInt().toString())
            PollutantItem(name = "CO", value = airQuality.co.toInt().toString())
            PollutantItem(name = "O₃", value = airQuality.o3.toInt().toString())
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Concentration in μg/m³",
            style = MaterialTheme.typography.bodySmall, // Match PollutantItem name style
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
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
 */
@Composable
private fun PollutantItem(name: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall, // smaller for de-emphasis
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
