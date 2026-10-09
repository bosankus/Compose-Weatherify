package bose.ankush.settings.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bose.ankush.settings.generated.resources.Res
import bose.ankush.settings.generated.resources.profile_change_photo_txt
import bose.ankush.settings.generated.resources.profile_photo_content_desc
import bose.ankush.settings.generated.resources.profile_remove_photo_txt
import org.jetbrains.compose.resources.stringResource

/**
 * Opened from the avatar's pencil once there is a photo: pick a new one, or remove it. Each
 * choice closes the sheet itself; removing still asks to confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfilePhotoOptionsSheet(
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SettingsSurface,
        contentColor = SettingsInk,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SettingsInkMuted) },
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                text = stringResource(Res.string.profile_photo_content_desc),
                color = SettingsInk,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            PhotoOption(
                icon = Icons.Outlined.PhotoLibrary,
                text = stringResource(Res.string.profile_change_photo_txt),
                tint = SettingsInk,
                onClick = onChangePhoto,
            )
            PhotoOption(
                icon = Icons.Outlined.Delete,
                text = stringResource(Res.string.profile_remove_photo_txt),
                tint = SettingsDanger,
                onClick = onRemovePhoto,
            )
        }
    }
}

@Composable
private fun PhotoOption(
    icon: ImageVector,
    text: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(text = text, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
