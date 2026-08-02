package bose.ankush.weatherify.wear.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

private val AlertAmber = Color(0xFFFFD54F)

@Composable
internal fun AlertDetailScreen(alert: AlertUiState) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val configuration = LocalConfiguration.current
    val marginFraction = if (configuration.isScreenRound) 0.08f else 0.04f
    val horizontalMargin = (configuration.screenWidthDp * marginFraction).dp

    ScreenScaffold(
        scrollState = columnState,
        // Alerts always use the low-glare night storm palette — the amber heading pops
        // against it regardless of the actual time of day.
        modifier = Modifier.background(
            brush = WeatherIconType.THUNDERSTORM.toBackgroundGradient(DayPhase.NIGHT),
        ),
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.padding(horizontal = horizontalMargin),
        ) {
            item {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = AlertAmber,
                    modifier = Modifier
                        .size(24.dp)
                        .transformedHeight(this, transformationSpec)
                )
            }
            item {
                Text(
                    text = alert.event,
                    style = MaterialTheme.typography.titleMedium,
                    color = AlertAmber,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(top = 4.dp)
                )
            }
            alert.sender?.let {
                item {
                    Text(
                        text = "Issued by: $it",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(top = 4.dp)
                    )
                }
            }
            if (alert.start != null || alert.end != null) {
                item {
                    val timeRange = listOfNotNull(alert.start, alert.end).joinToString(" - ")
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.bodyExtraSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(top = 4.dp)
                    )
                }
            }
            item {
                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(top = 12.dp)
                )
            }
        }
    }
}
