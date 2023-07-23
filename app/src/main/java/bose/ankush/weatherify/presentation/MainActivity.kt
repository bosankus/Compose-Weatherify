package bose.ankush.weatherify.presentation

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import bose.ankush.weatherify.base.common.Extension.hasLocationPermission
import bose.ankush.weatherify.base.common.Extension.openAppSystemSettings
import bose.ankush.weatherify.base.common.PERMISSIONS_TO_REQUEST
import bose.ankush.weatherify.base.common.startInAppUpdate
import bose.ankush.weatherify.base.permissions.CoarseLocationPermissionTextProvider
import bose.ankush.weatherify.base.permissions.FineLocationPermissionTextProvider
import bose.ankush.weatherify.base.permissions.PermissionAlertDialog
import bose.ankush.weatherify.presentation.navigation.AppNavigation
import bose.ankush.weatherify.presentation.theme.WeatherifyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        startInAppUpdate(this)

        setContent {
            WeatherifyTheme {
                val context: Context = LocalContext.current
                if (context.hasLocationPermission()) {
                    // if permission granted already then fetch and save location coordinates
                    viewModel.fetchAndSaveLocationCoordinates()
                } else {
                    // request location permission
                    RequestLocationPermission()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AppNavigation()
                }
            }
        }
    }

    @Composable
    fun RequestLocationPermission() {
        val context: Context = LocalContext.current
        val permissionQueue = viewModel.permissionDialogQueue

        val locationPermissionsResultLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissionMap ->
                    PERMISSIONS_TO_REQUEST.forEach { permission ->
                        viewModel.onPermissionResult(
                            permission = permission,
                            isGranted = permissionMap[permission] == true
                        )
                    }
                })

        permissionQueue.reversed().forEach { permission ->
            PermissionAlertDialog(permissionTextProvider = when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION -> FineLocationPermissionTextProvider()
                Manifest.permission.ACCESS_COARSE_LOCATION -> CoarseLocationPermissionTextProvider()
                else -> return@forEach
            },
                isPermanentlyDeclined = shouldShowRequestPermissionRationale(permission),
                onDismissClick = viewModel::dismissDialog,
                onOkClick = {
                    viewModel.dismissDialog()
                    locationPermissionsResultLauncher.launch(PERMISSIONS_TO_REQUEST)
                },
                onGoToAppSettingClick = { context.openAppSystemSettings() })
        }

        LaunchedEffect(key1 = Unit) {
            locationPermissionsResultLauncher.launch(PERMISSIONS_TO_REQUEST)
        }
    }

    override fun onResume() {
        super.onResume()
        startInAppUpdate(this)
    }
}
