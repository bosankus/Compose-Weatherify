package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WanderMetricChips(
    feel: String,
    wind: String,
    uv: String,
    modifier: Modifier = Modifier,
    contentColor: Color = WanderOnDark,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricChip(
            icon = Icons.Outlined.Thermostat,
            label = "Feel",
            value = feel,
            contentDescription = "Real feel",
            contentColor = contentColor,
            modifier = Modifier.weight(1f),
        )
        MetricChip(
            icon = Icons.Outlined.Air,
            label = "Wind",
            value = wind,
            contentDescription = "Wind",
            contentColor = contentColor,
            modifier = Modifier.weight(1f),
        )
        MetricChip(
            icon = Icons.Outlined.WbSunny,
            label = "UV",
            value = uv,
            contentDescription = "UV",
            contentColor = contentColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    label: String,
    value: String,
    contentDescription: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(chipFill, RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = contentColor,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = value,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }
}

private val chipFill = Color.Black.copy(alpha = 0.38f)
