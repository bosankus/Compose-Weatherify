package bose.ankush.home.presentation.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Not in the reference image. Material3 text fields, not a measured sheet.
 * Date is collected because [startsAt] has to be an ISO-8601 instant.
 */
@Composable
internal fun ShellCreateDialog(
    state: ShellState,
    canSave: Boolean,
    onIntent: (ShellIntent) -> Unit,
    onSave: () -> Unit,
) {
    if (!state.showCreate) return
    AlertDialog(
        onDismissRequest = { onIntent(ShellIntent.DismissCreate) },
        title = { Text("New event") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Nothing is saved until a title and a time are entered.")
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onIntent(ShellIntent.TitleChanged(it)) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.dateText,
                    onValueChange = { onIntent(ShellIntent.DateChanged(it)) },
                    label = { Text("Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.timeText,
                    onValueChange = { onIntent(ShellIntent.TimeChanged(it)) },
                    label = { Text("Time (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (state.placeName.isNullOrBlank()) {
                    Text("Current place isn't available yet.")
                }
                state.createError?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = canSave && !state.posting) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(ShellIntent.DismissCreate) }) {
                Text("Cancel")
            }
        },
    )
}
