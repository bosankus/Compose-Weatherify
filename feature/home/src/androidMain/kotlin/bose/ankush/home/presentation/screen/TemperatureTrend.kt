package bose.ankush.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact dark card: cooling, warming, or steady from the next few hours.
 * Callers must omit this entirely when [trend] would be null.
 */
@Composable
internal fun TemperatureTrend(
    trend: TemperatureTrendData,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CardFill, RoundedCornerShape(20.dp))
                .semantics(mergeDescendants = true) { contentDescription = trend.line }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = trend.direction.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        AnimatedValueText(
            text = trend.line,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
        )
    }
}

private val TemperatureTrendDirection.icon: ImageVector
    get() =
        when (this) {
            TemperatureTrendDirection.COOLING -> Icons.AutoMirrored.Outlined.TrendingDown
            TemperatureTrendDirection.WARMING -> Icons.AutoMirrored.Outlined.TrendingUp
            TemperatureTrendDirection.STEADY -> Icons.AutoMirrored.Outlined.TrendingFlat
        }

private val CardFill = Color.Black.copy(alpha = 0.38f)
