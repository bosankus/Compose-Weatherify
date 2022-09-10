package bose.ankush.weatherify.common

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import bose.ankush.dialog.DialogBox
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@ExperimentalPermissionsApi
object LocationPermissionManager {

    @Composable
    fun RequestPermission(
        actionPermissionGranted: () -> Unit,
        actionPermissionDenied: () -> Unit
    ) {
        val permissionStates = rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )

        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(key1 = lifecycleOwner, effect = {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START)
                    permissionStates.launchMultiplePermissionRequest()
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        })

        // Requesting permission for each from the list
        permissionStates.permissions.forEach { it ->
            when (it.permission) {
                Manifest.permission.ACCESS_COARSE_LOCATION -> {
                    when {
                        it.status.isGranted -> {
                            /* Permission has been granted by the user.
                               Hence getting the current lat, lang.
                            */
                            actionPermissionGranted()
                        }
                        it.status.shouldShowRationale -> {
                            /* Happens if a user denies the permission two times.
                               Hence needs to show why permission is required.
                               On action button click request permission.
                             */
                            MandatoryPermissionDescriptionContent(canClose = true) { actionPermissionDenied() }
                        }
                        !it.status.isGranted && !it.status.shouldShowRationale -> {
                            /* If the permission is denied and the should not show rationale
                               Hence we can only allow the permission manually through app settings.
                               On action button click go to settings page
                             */
                            MandatoryPermissionDescriptionContent(canClose = true) { actionPermissionDenied() }
                        }
                    }
                }
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    when {
                        it.status.isGranted -> {
                            /* Permission has been granted by the user.
                               Hence getting the current lat, lang.
                            */
                            actionPermissionGranted()
                        }
                        it.status.shouldShowRationale -> {
                            /* Happens if a user denies the permission two times.
                               Hence needs to show why permission is required.
                             */
                            MandatoryPermissionDescriptionContent(canClose = true) { actionPermissionDenied() }

                        }
                        !it.status.isGranted && !it.status.shouldShowRationale -> {
                            /* If the permission is denied and the should not show rationale
                               Hence we can only allow the permission manually through app settings
                             */
                            MandatoryPermissionDescriptionContent(canClose = true) { actionPermissionDenied() }

                        }
                    }
                }
            }
        }
    }


    @Composable
    fun MandatoryPermissionDescriptionContent(canClose: Boolean, action: () -> Unit) {
        val canCloseState = remember { mutableStateOf(canClose) }
        if (canCloseState.value) {
            DialogBox(
                headingText = "Location permission Required",
                descriptionText = "Location permission is applicable while showing you weather & air quality as per your current location. Without this permission few features won't be available. Click allow to open app settings screen for granting location permission.",
                dismissButtonText = "Deny",
                confirmButtonText = "Allow",
                confirmOnClick = { action() },
                closeOnClick = canCloseState
            )
        }
    }

}

