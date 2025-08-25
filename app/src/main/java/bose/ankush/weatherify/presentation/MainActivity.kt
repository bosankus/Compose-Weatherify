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
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import javax.inject.Inject

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var locationClient: LocationClient

    // Hold a reference to the Checkout instance only during payment
    private var razorpayCheckout: Checkout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        startInAppUpdate(this)

        setContent {
            WeatherifyTheme {
                val context = LocalContext.current
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val authState by viewModel.authState.collectAsState()
                val isAuthInitialized by viewModel.isAuthInitialized.collectAsState()

                // Set status bar color and icon color based on background
                @Suppress("DEPRECATION")
                val systemUiController = rememberSystemUiController()
                val bgColor = MaterialTheme.colorScheme.background
                val useDarkIcons = bgColor.luminance() > 0.5f
                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = Color.Transparent,
                        darkIcons = useDarkIcons
                    )
                }

                // Glassmorphic snackbar state
                val (showSnackbar, snackbarContent) = rememberGlassmorphicSnackbarState()

                // Handle authentication state changes
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Error -> {
                            showSnackbar((authState as AuthState.Error).message.asString(this@MainActivity))
                            viewModel.resetAuthState()
                        }
                        is AuthState.Success -> {
                            showSnackbar("Authentication successful")
                            viewModel.resetAuthState()
                        }
                        else -> Unit
                    }
                }

                // Listen for Unauthorized events
                LaunchedEffect(Unit) {
                    bose.ankush.network.auth.events.AuthEventBus.events.collect { event ->
                        if (event is bose.ankush.network.auth.events.AuthEvent.Unauthorized) {
                            showSnackbar(
                                event.message.ifBlank {
                                    "You need to log in again to continue using the app for security purposes."
                                }
                            )
                        }
                    }
                }

                // Collect payment events and launch Razorpay Checkout
                LaunchedEffect(Unit) {
                    viewModel.paymentEvents.collect { evt ->
                        if (evt is bose.ankush.weatherify.presentation.payment.PaymentEvent.LaunchCheckout) {
                            try {
                                Checkout.preload(applicationContext)
                                razorpayCheckout = Checkout()
                                razorpayCheckout?.setKeyID(evt.keyId)
                                val options = JSONObject().apply {
                                    put("name", evt.name)
                                    put("description", evt.description)
                                    put("order_id", evt.orderId)
                                    put("currency", evt.currency)
                                    put("amount", evt.amount)
                                    val prefill = JSONObject().apply {
                                        evt.email?.let { put("email", it) }
                                        evt.contact?.let { put("contact", it) }
                                    }
                                    put("prefill", prefill)
                                }
                                razorpayCheckout?.open(this@MainActivity, options)
                            } catch (e: Exception) {
                                viewModel.onPaymentFailed(
                                    e.message ?: "Unable to open payment checkout"
                                )
                                // Clean up in case of error
                                Checkout.clearUserData(context)
                                razorpayCheckout = null
                            }
                        }
                    }
                }

                // Main content
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
                            ) { CircularProgressIndicator() }
                        }
                        isLoggedIn -> {
                            // Only fetch location once after login, not on every recomposition
                            val launchNotificationPermissionState =
                                viewModel.launchNotificationPermission.collectAsState()
                            LaunchedEffect(isLoggedIn) {
                                if (locationClient.hasLocationPermission()) {
                                    viewModel.fetchAndSaveLocationCoordinates()
                                }
                            }
                            // If location permission is missing, request it on first launch
                            if (!locationClient.hasLocationPermission()) {
                                RequestLocationPermission(context)
                            }
                            if (launchNotificationPermissionState.value) {
                                RequestNotificationPermission(context)
                            }
                            LaunchedEffect(launchNotificationPermissionState.value) {
                                viewModel.updateShowNotificationBannerState(!context.hasNotificationPermission())
                            }
                            AppNavigation(viewModel)
                        }
                        else -> {
                            // Only show login screen if not logged in and auth is initialized
                            LoginScreen(
                                onLoginClick = { email, password ->
                                    viewModel.login(
                                        email,
                                        password
                                    )
                                },
                                onRegisterClick = { email, password ->
                                    viewModel.register(
                                        email,
                                        password
                                    )
                                },
                                onTermsClick = { context.openUrlInBrowser("https://data.androidplay.in/wfy/terms-and-conditions") },
                                onPrivacyPolicyClick = { context.openUrlInBrowser("https://data.androidplay.in/wfy/privacy-policy") },
                                isLoading = authState is AuthState.Loading
                            )
                        }
                    }
                    // Overlay glassmorphic snackbar
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) { snackbarContent() }
                }
            }
        }
    }

    /**
     * Request location permissions using Compose dialog and launcher.
     */
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
                isPermanentlyDeclined = !shouldShowRequestPermissionRationale(permission),
                onDismissClick = viewModel::dismissDialog,
                onOkClick = {
                    viewModel.dismissDialog()
                    locationPermissionsResultLauncher.launch(PERMISSIONS_TO_REQUEST)
                },
                onGoToAppSettingClick = { context.openAppSystemSettings() })
        }

        // Launch initial permission request if missing and queue is empty (first-launch scenario)
        LaunchedEffect(Unit) {
            if (permissionQueue.isEmpty() && !locationClient.hasLocationPermission()) {
                locationPermissionsResultLauncher.launch(PERMISSIONS_TO_REQUEST)
            }
        }

        // Also launch when there are items in the queue (e.g., after denial to show rationale)
        LaunchedEffect(permissionQueue.size) {
            if (permissionQueue.isNotEmpty()) {
                locationPermissionsResultLauncher.launch(PERMISSIONS_TO_REQUEST)
            }
        }
    }

    /**
     * Request notification permission using Compose launcher.
     */
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
                        viewModel.updateShowNotificationBannerState(false)
                    }
                }
            )
        LaunchedEffect(Unit) {
            notificationPermissionResultLauncher.launch(ACCESS_NOTIFICATION)
        }
    }

    override fun onResume() {
        super.onResume()
        startInAppUpdate(this)
    }

    /**
     * Razorpay payment success callback.
     */
    override fun onPaymentSuccess(razorpayPaymentID: String?, paymentData: PaymentData?) {
        val orderId = paymentData?.orderId.orEmpty()
        val paymentId = paymentData?.paymentId ?: razorpayPaymentID.orEmpty()
        val signature = paymentData?.signature.orEmpty()
        if (orderId.isNotBlank() && paymentId.isNotBlank() && signature.isNotBlank()) {
            viewModel.verifyPayment(orderId, paymentId, signature)
        } else {
            viewModel.onPaymentFailed("Payment succeeded but missing data")
        }
        razorpayCheckout = null
    }

    /**
     * Razorpay payment error callback.
     */
    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val message = response ?: "Payment failed with code $code"
        viewModel.onPaymentFailed(message)
        razorpayCheckout = null
    }

    override fun onDestroy() {
        super.onDestroy()
        razorpayCheckout = null
    }
}