package bose.ankush.dialog

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

/**
 * Preview of the dialog box UI.
 */
@SuppressLint("UnrememberedMutableState")
@Preview(name = "Dialog Box")
@Composable
fun DialogBoxPreview() {
    val dialogHeading = "Location permission"
    val dialogDesc = "This app requires location permission to provide best possible weather report at your location"
    val context = LocalContext.current
    val message = "This is sample message"
    val toastLength = Toast.LENGTH_SHORT
    DialogBox(
        icon = R.drawable.ic_info,
        headingText = dialogHeading,
        descriptionText = dialogDesc,
        negativeButtonText = "Deny",
        positiveButtonText = "Allow",
        positiveOnClick = { Toast.makeText(context, message, toastLength).show() },
        openDialogBox = mutableStateOf(false),
    )
}