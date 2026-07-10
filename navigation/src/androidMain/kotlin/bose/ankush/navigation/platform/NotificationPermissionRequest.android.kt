package bose.ankush.navigation.platform

import android.annotation.SuppressLint
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat

private const val ACCESS_NOTIFICATION = android.Manifest.permission.POST_NOTIFICATIONS

@SuppressLint("InlinedApi")
@Composable
actual fun RequestNotificationPermission(onResult: (isGranted: Boolean, isPermanentlyDeclined: Boolean) -> Unit) {
    val activity = LocalActivity.current
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                val isPermanentlyDeclined =
                    !isGranted &&
                        activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            ACCESS_NOTIFICATION,
                        )
                onResult(isGranted, isPermanentlyDeclined)
            },
        )
    LaunchedEffect(Unit) {
        launcher.launch(ACCESS_NOTIFICATION)
    }
}
