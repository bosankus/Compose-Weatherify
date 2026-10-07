package bose.ankush.home.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Slim "Updating" pill above the tab bar while a background weather refresh runs.
 * Not a tap target. Hidden when pull-to-refresh is already showing its own spinner.
 */
@Composable
internal fun RefreshChip(
    visible: Boolean,
    colors: MetricChipColors,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = REFRESH_CHIP_FADE_MS)),
        exit = fadeOut(tween(durationMillis = REFRESH_CHIP_FADE_MS)),
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .clearAndSetSemantics {
                        contentDescription = "Updating"
                        liveRegion = LiveRegionMode.Polite
                    }.background(colors.surface, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = colors.content,
                strokeWidth = 1.5.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Updating",
                color = colors.content,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private const val REFRESH_CHIP_FADE_MS = 150
