package bose.ankush.commonui.components

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

@Composable
fun WeatherHourCard(
    time: String,
    temperature: String,
    weatherDescription: String? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    iconContent: @Composable () -> Unit,
) {
    val selectedBackground = MaterialTheme.colorScheme.primaryContainer
    val unselectedBackground = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)

    Box(
        modifier =
            Modifier
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .background(if (isSelected) selectedBackground else unselectedBackground)
                .padding(horizontal = 10.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(0.6f),
            )

            iconContent()

            Text(
                text = temperature,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp),
            )

            weatherDescription?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(0.6f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
