package bose.ankush.weatherify.wear.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import bose.ankush.weatherify.wear.presentation.components.horizontalScreenMargin
import bose.ankush.weatherify.wear.presentation.theme.DialColors

@Composable
internal fun AlertDetailScreen(alert: AlertUiState) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = columnState) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.padding(horizontal = horizontalScreenMargin()),
        ) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .transformedHeight(this, transformationSpec)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DialColors.AccentGradient),
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = DialColors.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            item {
                Text(
                    text = alert.event,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = DialColors.TextPrimary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .padding(top = 8.dp),
                )
            }
            alert.sender?.let {
                item {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = DialColors.TextMuted,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .padding(top = 2.dp),
                    )
                }
            }
            if (alert.start != null || alert.end != null) {
                item {
                    Text(
                        text = listOfNotNull(alert.start, alert.end).joinToString(" – "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = DialColors.Crimson,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .padding(top = 6.dp),
                    )
                }
            }
            if (alert.description.isNotBlank()) {
                item {
                    Text(
                        text = alert.description,
                        fontSize = 13.sp,
                        color = DialColors.TextPrimary.copy(alpha = 0.85f),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
