package bose.ankush.weatherify.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Tracks the first-launch location-permission request queue shown by [MainActivity].
 * Survives configuration changes and is readable from both Compose and `onResume()`.
 */
@HiltViewModel
class AppPermissionViewModel
    @Inject
    constructor() : ViewModel() {
        var permissionDialogQueue = mutableStateListOf<String>()
            private set

        fun dismissDialog() {
            if (permissionDialogQueue.isNotEmpty()) {
                permissionDialogQueue.removeAt(0)
            }
        }

        fun onPermissionResult(
            permission: String,
            isGranted: Boolean,
        ) {
            if (!isGranted && !permissionDialogQueue.contains(permission)) {
                permissionDialogQueue.add(permission)
            }
        }

        fun removeGrantedPermissions(grantedPermissions: List<String>) {
            grantedPermissions.forEach { permissionDialogQueue.remove(it) }
        }
    }
