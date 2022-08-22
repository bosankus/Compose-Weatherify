package bose.ankush.weatherify.common

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import bose.ankush.dialog.DialogBox
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@ExperimentalPermissionsApi
object PermissionManager {

    @Composable
    fun RequestPermission(
        permissions: List<String>,
        onPermissionGranted: () -> Unit,
    ) {
        val permissionState = rememberMultiplePermissionsState(permissions)
        

        HandlePermissionRequest(
            permissionState = permissionState,
            deniedContent = { shouldShowReason ->
                PermissionDeniedContent(
                    shouldShowReason = shouldShowReason,
                    onRequestPermission = { permissionState.launchMultiplePermissionRequest() }
                )
            },
            allowedContent = { onPermissionGranted() },
        )
    }


    @SuppressLint("PermissionLaunchedDuringComposition")
    @Composable
    fun HandlePermissionRequest(
        permissionState: MultiplePermissionsState,
        deniedContent: @Composable (Boolean) -> Unit,
        allowedContent: @Composable () -> Unit,
    ) {
        if (permissionState.allPermissionsGranted) allowedContent()
        else deniedContent(permissionState.shouldShowRationale)
    }


    @Composable
    fun PermissionDeniedContent(
        shouldShowReason: Boolean,
        onRequestPermission: () -> Unit
    ) {
        if (shouldShowReason) {
            val enableLocation = remember { mutableStateOf(true) }
            DialogBox(
                headingText = "Mandatory Location Permission",
                descriptionText = "This permission is mandatory for the app to function properly. Please allow it to continue.",
                dismissButtonText = "",
                confirmButtonText = "Allow",
                confirmOnClick = { onRequestPermission() },
                dialogState = enableLocation
            )
        } else Content(onClick = onRequestPermission)
    }


    @Composable
    fun Content(
        showButton: Boolean = true,
        onClick: () -> Unit
    ) {
        if (showButton) {
            val enableLocation = remember { mutableStateOf(true) }
            if (enableLocation.value) {
                DialogBox(
                    headingText = "Location Permission Request",
                    descriptionText = "Location permission is required to show you the weather as per your current area. Without this permission this feature won't be available",
                    dismissButtonText = "Deny",
                    confirmButtonText = "Allow",
                    confirmOnClick = { onClick() },
                    dialogState = enableLocation
                )
            }
        }
    }
}
