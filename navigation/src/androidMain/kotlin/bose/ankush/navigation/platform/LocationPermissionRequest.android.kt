package bose.ankush.navigation.platform

import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat

private val LOCATION_PERMISSIONS =
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

@Composable
actual fun RequestLocationPermission(onResult: (isGranted: Boolean, isPermanentlyDeclined: Boolean) -> Unit) {
    val activity = LocalActivity.current
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { grants ->
                val isGranted = grants.values.all { it }
                val isPermanentlyDeclined =
                    !isGranted &&
                        activity != null &&
                        LOCATION_PERMISSIONS.all { permission ->
                            grants[permission] == true ||
                                !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                        }
                onResult(isGranted, isPermanentlyDeclined)
            },
        )
    LaunchedEffect(Unit) {
        launcher.launch(LOCATION_PERMISSIONS)
    }
}
