package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WanderCalendarStrip(
    days: List<WanderCalendarDay>,
    modifier: Modifier = Modifier,
    contentColor: Color = WanderOnDark,
) {
    if (days.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Event calendar",
            color = contentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            days.forEach { day ->
                DayCell(day = day, contentColor = contentColor)
            }
        }
    }
}

@Composable
private fun DayCell(
    day: WanderCalendarDay,
    contentColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.label,
            color = contentColor,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (day.selected) Color.White else Color.Transparent),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day.date,
                color = if (day.selected) WanderOnLight else contentColor,
                fontSize = 16.sp,
                fontWeight = if (day.selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        if (day.temp.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = day.temp,
                color = contentColor,
                fontSize = 13.sp,
            )
        }
    }
}
