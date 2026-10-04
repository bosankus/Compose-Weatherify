package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val days =
    listOf(
        "Mon" to "5",
        "Tue" to "6",
        "Wed" to "7",
        "Thu" to "8",
        "Fri" to "9",
        "Sat" to "10",
        "Sun" to "11",
    )

@Composable
fun WanderCalendarStrip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        days.forEachIndexed { index, (label, date) ->
            DayCell(
                label = label,
                date = date,
                selected = index == SELECTED_DAY_INDEX,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(
    label: String,
    date: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) selectedDayFill else Color.Transparent)
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = dayLabel,
            fontSize = 11.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = date,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private const val SELECTED_DAY_INDEX = 2

private val selectedDayFill = Color.White.copy(alpha = 0.16f)
private val dayLabel = Color.White.copy(alpha = 0.7f)
