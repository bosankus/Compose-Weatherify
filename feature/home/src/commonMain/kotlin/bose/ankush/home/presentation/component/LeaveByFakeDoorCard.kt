package bose.ankush.home.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
 * Android leave-by fake door. One existing Material icon, no animation, no map,
 * no extra artwork. Joining and "was this misleading?" stay on this device.
 */
@Composable
internal fun LeaveByFakeDoorCard(
    hasJoined: Boolean,
    hasNotedMisleading: Boolean,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
    onMisleading: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Thunderstorm,
                    contentDescription = stringResource(Res.string.leave_by_icon_content_desc),
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.leave_by_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.leave_by_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (hasJoined) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.leave_by_joined_confirmation),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (!hasJoined) {
                Button(
                    onClick = onJoin,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.leave_by_primary))
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.leave_by_dismiss))
            }
            if (hasNotedMisleading) {
                Text(
                    text = stringResource(Res.string.leave_by_misleading_noted),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            } else {
                TextButton(
                    onClick = onMisleading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.leave_by_misleading))
                }
            }
        }
    }
}
