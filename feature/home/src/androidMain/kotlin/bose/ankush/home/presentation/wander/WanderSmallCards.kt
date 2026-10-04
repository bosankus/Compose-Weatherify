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
fun WanderSmallCards(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        SmallCard(
            title = "Sunrise",
            value = "6:30 AM",
            modifier = Modifier.weight(1f).padding(end = 6.dp),
        )
        SmallCard(
            title = "Sunset",
            value = "9:00 PM",
            modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
    }
}

@Composable
private fun SmallCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(cardFill, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            color = cardLabel,
            fontSize = 12.sp,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private val cardFill = Color.White.copy(alpha = 0.14f)
private val cardLabel = Color.White.copy(alpha = 0.7f)
