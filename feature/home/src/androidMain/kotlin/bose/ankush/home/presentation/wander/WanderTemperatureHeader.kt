package bose.ankush.home.presentation.wander

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WanderTemperatureHeader(
    temperature: String,
    place: String,
    conditionLine: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = temperature,
            color = Color.White,
            fontSize = 96.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 96.sp,
            letterSpacing = (-1.5).sp,
        )
        Text(
            text = place,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = conditionLine,
            color = conditionLineColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

private val conditionLineColor = Color.White.copy(alpha = 0.82f)
