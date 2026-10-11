package bose.ankush.home.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.ToastOnWarning
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.ai_summary_action
import bose.ankush.home.generated.resources.ai_summary_body
import bose.ankush.home.generated.resources.ai_summary_title
import org.jetbrains.compose.resources.stringResource

/**
 * Entry row for the weather summary. Layout only: it does nothing until [onClick] is wired.
 * Same fill and radius as the other home rows.
 */
@Composable
fun AiSummaryEntry(
    modifier: Modifier = Modifier,
    contentColor: Color = ContentOnDark,
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(entryFill)
                .clickable(
                    onClickLabel = stringResource(Res.string.ai_summary_action),
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = WarningYellow,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.ai_summary_title),
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(Res.string.ai_summary_body),
                color = contentColor,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(WarningYellow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = ToastOnWarning,
            )
        }
    }
}

private val entryFill = Color.Black.copy(alpha = 0.38f)
