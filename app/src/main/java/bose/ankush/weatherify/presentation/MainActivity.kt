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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import bose.ankush.sunriseui.auth.LoginScreen
import bose.ankush.sunriseui.components.rememberGlassmorphicSnackbarState
import bose.ankush.weatherify.base.common.ACCESS_NOTIFICATION
import bose.ankush.weatherify.base.common.Extension.hasNotificationPermission
import bose.ankush.weatherify.base.common.Extension.openAppSystemSettings
import bose.ankush.weatherify.base.common.Extension.openUrlInBrowser
import bose.ankush.weatherify.base.common.PERMISSIONS_TO_REQUEST
import bose.ankush.weatherify.base.common.startInAppUpdate
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.base.permissions.CoarseLocationPermissionTextProvider
import bose.ankush.weatherify.base.permissions.FineLocationPermissionTextProvider
import bose.ankush.weatherify.base.permissions.PermissionAlertDialog
import bose.ankush.weatherify.presentation.navigation.AppNavigation
import bose.ankush.weatherify.presentation.theme.WeatherifyTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
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
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        startInAppUpdate(this)

        setContent {
            WeatherifyTheme {
                val context: Context = LocalContext.current
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val authState by viewModel.authState.collectAsState()
                val isAuthInitialized by viewModel.isAuthInitialized.collectAsState()

                // Configure status bar to be transparent and adjust icon colors based on background luminance
                val systemUiController = rememberSystemUiController()
                val bgColor = MaterialTheme.colorScheme.background
                val useDarkIcons = bgColor.luminance() > 0.5f
                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }

                // Create a state for the glassmorphic snackbar
                val (showSnackbar, snackbarContent) = rememberGlassmorphicSnackbarState()

                // Handle authentication state changes
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Error -> {
                            // Show error message in glassmorphic snackbar
                            val errorMessage =
                                (authState as AuthState.Error).message.asString(this@MainActivity)
                            showSnackbar(errorMessage)

                            // Reset auth state after showing error
                            viewModel.resetAuthState()
                        }

                        is AuthState.Success -> {
                            // Show success message in glassmorphic snackbar
                            showSnackbar("Authentication successful")

                            // Reset auth state after successful authentication
                            // The isLoggedIn state will be updated automatically by the authRepository
                            viewModel.resetAuthState()
                        }

                        else -> {
                            // Do nothing for other states
                        }
                    }
                }

                // Main content box that contains everything
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                ) {
                    when {
                        !isAuthInitialized -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        isLoggedIn -> {
                            // User is logged in, show main app content
                            val launchNotificationPermissionState =
                                viewModel.launchNotificationPermission.collectAsState()
                            if (locationClient.hasLocationPermission()) {
                                // if permission granted already then fetch and save location coordinates
                                viewModel.fetchAndSaveLocationCoordinates()
                            } else {
                                // request location permission
                                RequestLocationPermission(context)
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
                            AppNavigation(viewModel)
                        }

                        else -> {
                            // User is not logged in, show login screen
                            LoginScreen(
                                onLoginClick = { email, password ->
                                    viewModel.login(email, password)
                                },
                                onRegisterClick = { email, password ->
                                    viewModel.register(email, password)
                                },
                                onTermsClick = {
                                    // Open terms and conditions in the default browser
                                    context.openUrlInBrowser("https://data.androidplay.in/wfy/terms-and-conditions")
                                },
                                onPrivacyPolicyClick = {
                                    // Open privacy policy in the default browser
                                    context.openUrlInBrowser("https://data.androidplay.in/wfy/privacy-policy")
                                },
                                isLoading = authState is AuthState.Loading
                            )
                        }
                    }

                    // Overlay the glassmorphic snackbar on top of all content
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        snackbarContent()
                    }
                }
            }
        }
    }

    @Composable
    fun RequestLocationPermission(context: Context) {
        val permissionQueue = viewModel.permissionDialogQueue

        val locationPermissionsResultLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissionMap ->
                    PERMISSIONS_TO_REQUEST.forEach { permission ->
                        viewModel.onPermissionResult(
                            permission = permission,
                            isGranted = permissionMap[permission] == true
                        )
                    }
                })

        permissionQueue.reversed().forEach { permission ->
            PermissionAlertDialog(
                permissionTextProvider = when (permission) {
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
    fun RequestNotificationPermission(context: Context) {
        val notificationPermissionResultLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
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