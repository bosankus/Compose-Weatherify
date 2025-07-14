package bose.ankush.sunriseui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A composable that displays weather information for a specific hour.
 * This component is designed to be flexible and reusable across different platforms.
 *
 * @param time The formatted time (e.g., "12:00 PM")
 * @param temperature The temperature value with unit
 * @param weatherDescription Optional description of the weather conditions
 * @param isSelected Whether this hour card is currently selected
 * @param onClick Callback for when the card is clicked
 * @param iconContent Composable content for the weather icon
 */
@Composable
fun WeatherHourCard(
    time: String,
    temperature: String,
    weatherDescription: String? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    iconContent: @Composable () -> Unit
) {
    // Pre-calculate background colors
    val selectedBackground = MaterialTheme.colorScheme.primaryContainer
    val unselectedBackground = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) selectedBackground else unselectedBackground)
            .padding(horizontal = 10.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(0.6f),
            )

            // Weather icon
            iconContent()

            // Temperature
            Text(
                text = temperature,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Weather description (if available)
            weatherDescription?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}