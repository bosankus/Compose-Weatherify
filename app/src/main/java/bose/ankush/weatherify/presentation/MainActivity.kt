package bose.ankush.weatherify.presentation

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import bose.ankush.weatherify.base.common.ACCESS_NOTIFICATION
import bose.ankush.weatherify.base.common.ACCESS_PHONE_CALL
import bose.ankush.weatherify.base.common.Extension.callNumber
import bose.ankush.weatherify.base.common.Extension.hasNotificationPermission
import bose.ankush.weatherify.base.common.Extension.openAppSystemSettings
import bose.ankush.weatherify.base.common.PERMISSIONS_TO_REQUEST
import bose.ankush.weatherify.base.common.startInAppUpdate
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.base.permissions.CoarseLocationPermissionTextProvider
import bose.ankush.weatherify.base.permissions.FineLocationPermissionTextProvider
import bose.ankush.weatherify.base.permissions.PermissionAlertDialog
import bose.ankush.weatherify.presentation.navigation.AppNavigation
import bose.ankush.weatherify.presentation.theme.WeatherifyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var locationClient: LocationClient

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        startInAppUpdate(this)

        setContent {
            WeatherifyTheme {
                val context: Context = LocalContext.current
                val launchPhoneCallPermissionState =
                    viewModel.launchPhoneCallPermission.collectAsState()
                val launchNotificationPermissionState =
                    viewModel.launchNotificationPermission.collectAsState()
                if (locationClient.hasLocationPermission()) {
                    // if permission granted already then fetch and save location coordinates
                    viewModel.fetchAndSaveLocationCoordinates()
                } else {
                    // request location permission
                    RequestLocationPermission(context)
                }
                if (launchPhoneCallPermissionState.value) {
                    // request phone call permission
                    RequestPhoneCallPermission(context)
                }
                if (launchNotificationPermissionState.value) {
                    // request notification permission
                    RequestNotificationPermission(context)
                }

                /**
                 * For Settings screen:
                 * notification item should be invisible if notification permission is already granted.
                 */
                LaunchedEffect(key1 = launchNotificationPermissionState) {
                    if (!context.hasNotificationPermission()) {
                        viewModel.updateShowNotificationBannerState(true)
                    } else {
                        viewModel.updateShowNotificationBannerState(false)
                    }
                }

                // main container holding all app composable screens
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }

    @Composable
    fun RequestLocationPermission(context: Context) {
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

    @Composable
    fun RequestPhoneCallPermission(context: Context) {
        val phoneCallPermissionResultLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    // TODO: Hardcoded task to call phone number as it is triggered from 1 place [AppNavigation]
                    if (isGranted) context.callNumber()
                }
            )

        LaunchedEffect(key1 = Unit) {
            phoneCallPermissionResultLauncher.launch(ACCESS_PHONE_CALL)
        }
    }

    @Composable
    fun RequestNotificationPermission(context: Context) {
        val notificationPermissionResultLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (isGranted) {
                        Toast.makeText(
                            context,
                            "Notification permission granted",
                            Toast.LENGTH_SHORT
                        ).show()
                        // hide notification banner on settings screen
                        viewModel.updateShowNotificationBannerState(false)
                    }
                }
            )

        LaunchedEffect(key1 = Unit) {
            notificationPermissionResultLauncher.launch(ACCESS_NOTIFICATION)
        }
    }

    override fun onResume() {
        super.onResume()
        startInAppUpdate(this)
    }
}
