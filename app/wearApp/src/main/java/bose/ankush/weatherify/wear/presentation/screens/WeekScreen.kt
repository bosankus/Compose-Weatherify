package bose.ankush.weatherify.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import bose.ankush.weatherify.wear.presentation.DailyUiState
import bose.ankush.weatherify.wear.presentation.components.horizontalScreenMargin
import bose.ankush.weatherify.wear.presentation.components.toImageVector
import bose.ankush.weatherify.wear.presentation.theme.DialColors

/** The coming days as a scrolling list: a red badge for the condition, the day, and its range. */
@Composable
internal fun WeekScreen(days: List<DailyUiState>) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = columnState) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalScreenMargin()),
        ) {
            item {
                Text(
                    text = "THIS WEEK",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    color = DialColors.Crimson,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(bottom = 4.dp),
                )
            }
            items(days.size) { index ->
                DayRow(
                    day = days[index],
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                )
            }
        }
    }
}

@Composable
private fun DayRow(
    day: DailyUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(CircleShape)
                .background(DialColors.Tile)
                .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DialColors.AccentGradient),
        ) {
            Icon(
                imageVector = day.icon.toImageVector(),
                contentDescription = null,
                tint = DialColors.TextPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = day.day.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = DialColors.TextPrimary,
            )
            day.condition?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = DialColors.TextMuted,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = day.high ?: "--",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DialColors.TextPrimary,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = day.low ?: "--",
            fontSize = 13.sp,
            color = DialColors.TextMuted,
        )
    }
}
