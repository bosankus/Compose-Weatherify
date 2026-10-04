package bose.ankush.home.presentation.wander

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.commonui.theme.WarningYellow
import bose.ankush.home.generated.resources.Res
import bose.ankush.home.generated.resources.leave_by_body
import bose.ankush.home.generated.resources.leave_by_dismiss
import bose.ankush.home.generated.resources.leave_by_icon_content_desc
import bose.ankush.home.generated.resources.leave_by_joined_confirmation
import bose.ankush.home.generated.resources.leave_by_misleading
import bose.ankush.home.generated.resources.leave_by_misleading_noted
import bose.ankush.home.generated.resources.leave_by_primary
import bose.ankush.home.generated.resources.leave_by_title
import org.jetbrains.compose.resources.stringResource

/**
 * One dark rounded row, same fill and radius as the other wander rows.
 * Copy and taps are the existing leave-by strings and ViewModel events.
 */
@Composable
fun WanderLeaveByRow(
    leaveBy: WanderLeaveBy,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(rowFill, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Thunderstorm,
                contentDescription = stringResource(Res.string.leave_by_icon_content_desc),
                modifier = Modifier.size(16.dp),
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.leave_by_title),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(Res.string.leave_by_body),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
        if (leaveBy.hasJoined) {
            Text(
                text = stringResource(Res.string.leave_by_joined_confirmation),
                color = WarningYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!leaveBy.hasJoined) {
                ActionText(text = stringResource(Res.string.leave_by_primary), onClick = leaveBy.onJoin)
            }
            ActionText(text = stringResource(Res.string.leave_by_dismiss), onClick = leaveBy.onDismiss)
            if (leaveBy.hasNotedMisleading) {
                Text(
                    text = stringResource(Res.string.leave_by_misleading_noted),
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            } else {
                ActionText(text = stringResource(Res.string.leave_by_misleading), onClick = leaveBy.onMisleading)
            }
        }
    }
}

@Composable
private fun ActionText(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

private val rowFill = Color.Black.copy(alpha = 0.38f)
