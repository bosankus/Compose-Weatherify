package bose.ankush.weatherify.base.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionAlertDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismissClick: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = "Permissions required") },
        text = { Text(text = permissionTextProvider.getDescription(isPermanentlyDeclined)) },
        confirmButton = {
            TextButton(onClick = if (isPermanentlyDeclined) onGoToAppSettingClick else onOkClick) {
                Text(text = if(isPermanentlyDeclined) "Grant Permission" else "Ok")
            }
        },
        dismissButton = {  }
    )
}
