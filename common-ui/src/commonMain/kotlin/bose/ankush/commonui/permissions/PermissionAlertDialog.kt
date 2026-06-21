package bose.ankush.commonui.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * A CMP-compatible permission alert dialog.
 *
 * The caller is responsible for:
 * - Providing localised [descriptionText] based on permission state
 * - Providing meaningful [positiveButtonLabel] / [negativeButtonLabel]
 * - Handling back-press behaviour (e.g. via BackHandler in the host composable)
 *
 * @param descriptionText       Body text shown inside the dialog.
 * @param isPermanentlyDeclined When true the negative (Exit/Cancel) button is shown and
 *                              tapping outside the dialog triggers [onNegativeAction].
 * @param onPositiveAction      Called when the confirm button is tapped.
 * @param onNegativeAction      Called when the dismiss button is tapped or the dialog is
 *                              dismissed by an outside tap (only when [isPermanentlyDeclined]).
 * @param positiveButtonLabel   Label for the confirm button. Defaults to "OK".
 * @param negativeButtonLabel   Label for the dismiss button. Defaults to "Cancel".
 */
@Composable
fun PermissionAlertDialog(
    descriptionText: String,
    isPermanentlyDeclined: Boolean,
    onPositiveAction: () -> Unit,
    onNegativeAction: () -> Unit,
    positiveButtonLabel: String = "OK",
    negativeButtonLabel: String = "Cancel",
) {
    AlertDialog(
        onDismissRequest = if (isPermanentlyDeclined) onNegativeAction else onPositiveAction,
        title = { Text(text = "Permissions required") },
        text = { Text(text = descriptionText) },
        confirmButton = {
            TextButton(onClick = onPositiveAction) {
                Text(text = positiveButtonLabel)
            }
        },
        dismissButton = {
            if (isPermanentlyDeclined) {
                TextButton(onClick = onNegativeAction) {
                    Text(text = negativeButtonLabel)
                }
            }
        },
    )
}
