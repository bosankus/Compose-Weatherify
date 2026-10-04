package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WanderMetricChips(
    feel: String,
    wind: String,
    uv: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        MetricChip(label = "Feel", value = feel, modifier = Modifier.weight(1f))
        MetricChip(label = "Wind", value = wind, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        MetricChip(label = "UV", value = uv, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(chipFill, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = chipLabel,
            fontSize = 12.sp,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private val chipFill = Color.White.copy(alpha = 0.14f)
private val chipLabel = Color.White.copy(alpha = 0.7f)
