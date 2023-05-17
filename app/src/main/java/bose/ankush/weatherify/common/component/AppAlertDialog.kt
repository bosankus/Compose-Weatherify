package bose.ankush.weatherify.common.component

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppAlertDialog(
    heroIcon: ImageVector,
    title: String,
    body: String,
    confirmBtnText: String,
    confirmBtnAction: () -> Unit,
    dismissBtnText: String = "Dismiss",
    exitApp: Boolean = false
) {

    val shouldExit = remember { mutableStateOf(exitApp) }

    // If user clicks on dismiss button, app should exist
    if (shouldExit.value) {
        val context = LocalContext.current as AppCompatActivity
        AlertDialog(
            onDismissRequest = { /*User can't close dialog when clicked outside*/ },
            title = { Text(text = title) },
            text = { Text(text = body) },
            icon = { Icon(imageVector = heroIcon, contentDescription = null) },
            confirmButton = {
                TextButton(onClick = confirmBtnAction) {
                    Text(text = confirmBtnText)
                }
            },
            dismissButton = {
                TextButton(onClick = { context.finish() }) {
                    Text(text = dismissBtnText)
                }
            }
        )
    }

}