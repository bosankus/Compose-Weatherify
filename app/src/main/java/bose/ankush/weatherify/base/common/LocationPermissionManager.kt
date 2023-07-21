package bose.ankush.weatherify.base.common

import android.Manifest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import bose.ankush.weatherify.base.common.component.AppAlertDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@ExperimentalPermissionsApi
object LocationPermissionManager {

    @Composable
    fun RequestPermission(
        actionPermissionGranted: () -> Unit,
        actionPermissionDenied: () -> Unit,
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
                            /** Permission has been granted by the user.
                            Hence getting the current lat, lang.
                             */
                            actionPermissionGranted()
                        }

                        it.status.shouldShowRationale -> {
                            /** Happens if a user denies the permission two times.
                            Hence needs to show why permission is required.
                            On action button click request permission.
                             */
                            MandatoryPermissionDescriptionContent(
                                confirmAction = actionPermissionDenied,
                                shouldExitIfDialogDismissed = true
                            )
                        }

                        !it.status.isGranted && !it.status.shouldShowRationale -> {
                            /** If the permission is denied and the should not show rationale
                            Hence we can only allow the permission manually through app settings.
                            On action button click go to settings page
                             */
                            MandatoryPermissionDescriptionContent(
                                confirmAction = actionPermissionDenied,
                                shouldExitIfDialogDismissed = true
                            )
                        }
                    }
                }

                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    when {
                        it.status.isGranted -> {
                            /** Permission has been granted by the user.
                            Hence getting the current lat, lang.
                             */
                            actionPermissionGranted()
                        }

                        it.status.shouldShowRationale -> {
                            /** Happens if a user denies the permission two times.
                            Hence needs to show why permission is required.
                             */
                            MandatoryPermissionDescriptionContent(
                                confirmAction = actionPermissionDenied,
                                shouldExitIfDialogDismissed = true
                            )

                        }

                        !it.status.isGranted && !it.status.shouldShowRationale -> {
                            /** If the permission is denied and the should not show rationale
                            Hence we can only allow the permission manually through app settings
                             */
                            MandatoryPermissionDescriptionContent(
                                confirmAction = actionPermissionDenied,
                                shouldExitIfDialogDismissed = true
                            )
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun MandatoryPermissionDescriptionContent(
        confirmAction: () -> Unit,
        shouldExitIfDialogDismissed: Boolean
    ) {
        AppAlertDialog(
            heroIcon = Icons.Outlined.LocationOn,
            title = "Location permission required",
            body = "Location permission is applicable while showing weather & air quality as per current location. Click allow to open app location settings.",
            confirmBtnText = "Allow",
            confirmBtnAction = confirmAction,
            dismissBtnText = "Exit",
            exitApp = shouldExitIfDialogDismissed
        )
    }

}

