package bose.ankush.sunriseui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A composable that displays weather alert information.
 * Shows a summarized alert by default and can be expanded to show more details.
 */
@Composable
fun WeatherAlertCard(
    title: String?,
    description: String?,
    startTime: Long?,
    endTime: Long?,
    source: String?,
    onReadMoreClick: (() -> Unit)? = null,
    initiallyExpanded: Boolean = false
) {
    // Skip rendering if essential data is missing
    if (title.isNullOrEmpty() || description.isNullOrEmpty()) return

    // State and calculated values
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    // Define colors
    val primaryColor = MaterialTheme.colorScheme.error
    val textColor = MaterialTheme.colorScheme.onErrorContainer
    val accentColor = primaryColor.copy(alpha = 0.8f)
    val surfaceColor = textColor.copy(alpha = 0.07f)
    val subtleTextColor = textColor.copy(alpha = 0.7f)

    // Create colors object
    val colors = AlertCardColors(
        primaryColor = primaryColor,
        textColor = textColor,
        accentColor = accentColor,
        surfaceColor = surfaceColor,
        subtleTextColor = subtleTextColor
    )

    // Format timestamps
    val formattedStartTime = remember(startTime) {
        startTime?.let { formatTimestamp(it) } ?: "Unknown"
    }
    val formattedEndTime = remember(endTime) {
        endTime?.let { formatTimestamp(it) } ?: "Unknown"
    }

    // Create short description
    val shortDescription = remember(description) {
        if (description.length > 100) description.take(100) + "..." else description
    }

    // Animation spec for content size changes
    val contentSizeAnimSpec = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .animateContentSize(animationSpec = contentSizeAnimSpec),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Header section with icon, title and timestamp
            AlertHeader(
                title = title,
                timestamp = formattedStartTime,
                colors = colors
            )

            // Report section with expandable description
            AlertReportSection(
                description = description,
                shortDescription = shortDescription,
                isExpanded = isExpanded,
                colors = colors,
                onToggleExpanded = {
                    isExpanded = !isExpanded
                    if (isExpanded && onReadMoreClick != null) {
                        onReadMoreClick()
                    }
                }
            )

            // Expanded content with source and validity
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                        expandVertically(tween(350, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(200)) +
                        shrinkVertically(tween(250))
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Source information
                    AlertInfoSection(
                        title = "Source",
                        content = source ?: "Unknown",
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Validity information
                    AlertInfoSection(
                        title = "Valid Until",
                        content = formattedEndTime,
                        colors = colors
                    )
                }
            }
        }
    }
}

/**
 * Header section of the alert card with icon, title and timestamp
 */
@Composable
private fun AlertHeader(
    title: String?,
    timestamp: String,
    colors: AlertCardColors
) {
    // Alert Icon
    Icon(
        imageVector = Icons.Filled.Warning,
        contentDescription = "Weather Alert Icon",
        tint = colors.primaryColor,
        modifier = Modifier
            .size(32.dp)
            .padding(bottom = 12.dp)
    )

    // Title
    Text(
        text = title ?: "Weather Alert",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textColor,
        modifier = Modifier.padding(bottom = 4.dp)
    )

    // Timestamp
    Text(
        text = "Issued: $timestamp",
        style = MaterialTheme.typography.bodySmall,
        color = colors.subtleTextColor,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

/**
 * Report section with expandable description and read more/less button
 */
@Composable
private fun AlertReportSection(
    description: String,
    shortDescription: String,
    isExpanded: Boolean,
    colors: AlertCardColors,
    onToggleExpanded: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Report label
            Text(
                text = "Report",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.accentColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Description text
            Text(
                text = if (isExpanded) description else shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textColor,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f,
                overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                modifier = Modifier.semantics {
                    contentDescription = "Alert description: $description"
                }
            )

            // Read more/less button
            val readMoreText = if (isExpanded) "Read less" else "Read more"
            val buttonAlpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "Button Alpha"
            )

            TextButton(
                onClick = onToggleExpanded,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
                    .alpha(buttonAlpha)
                    .semantics {
                        contentDescription = if (isExpanded)
                            "Read less about this alert"
                        else
                            "Read more about this alert"
                    }
            ) {
                Text(
                    text = readMoreText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.primaryColor
                )
            }
        }
    }
}

/**
 * Reusable section for displaying information with a title and content
 */
@Composable
private fun AlertInfoSection(
    title: String,
    content: String,
    colors: AlertCardColors
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.accentColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textColor
            )
        }
    }
}

/**
 * Data class to hold color values for the alert card
 */
private data class AlertCardColors(
    val primaryColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val surfaceColor: Color,
    val subtleTextColor: Color
)

/**
 * Formats a timestamp into a readable date and time string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp * 1000) // Convert to milliseconds
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Preview of the WeatherAlertCard in collapsed state.
 */
@Preview(showBackground = true)
@Composable
private fun WeatherAlertCardPreviewCollapsed() {
    MaterialTheme {
        WeatherAlertCard(
            title = "Severe Thunderstorm Warning",
            description = "The National Weather Service has issued a severe thunderstorm warning for your area. Expect heavy rain, strong winds, and possible hail. Take necessary precautions and stay indoors if possible.",
            startTime = System.currentTimeMillis() / 1000,
            endTime = (System.currentTimeMillis() / 1000) + 3600 * 3, // 3 hours later
            source = "National Weather Service"
        )
    }
}

/**
 * Preview of the WeatherAlertCard in expanded state.
 */
@Preview(showBackground = true)
@Composable
private fun WeatherAlertCardPreviewExpanded() {
    MaterialTheme {
        WeatherAlertCard(
            title = "Severe Thunderstorm Warning",
            description = "The National Weather Service has issued a severe thunderstorm warning for your area. Expect heavy rain, strong winds, and possible hail. Take necessary precautions and stay indoors if possible.",
            startTime = System.currentTimeMillis() / 1000,
            endTime = (System.currentTimeMillis() / 1000) + 3600 * 3, // 3 hours later
            source = "National Weather Service",
            initiallyExpanded = true
        )
    }
}