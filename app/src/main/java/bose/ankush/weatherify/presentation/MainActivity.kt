package bose.ankush.weatherify.presentation

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import bose.ankush.commonui.auth.LoginScreen
import bose.ankush.commonui.permissions.PermissionAlertDialog
import bose.ankush.commonui.components.NotificationToast
import bose.ankush.commonui.components.ToastType
import bose.ankush.commonui.components.rememberToastAnchorState
import bose.ankush.payment.presentation.PaymentViewModel
import bose.ankush.weatherify.base.common.ACCESS_NOTIFICATION
import bose.ankush.weatherify.base.common.Extension.hasNotificationPermission
import bose.ankush.weatherify.base.common.Extension.openAppSystemSettings
import bose.ankush.weatherify.base.common.PERMISSIONS_TO_REQUEST
import bose.ankush.weatherify.base.common.startInAppUpdate
import bose.ankush.weatherify.base.location.LocationClient
import bose.ankush.weatherify.base.permissions.CoarseLocationPermissionTextProvider
import bose.ankush.weatherify.base.permissions.FineLocationPermissionTextProvider
import bose.ankush.weatherify.presentation.navigation.AppNavigation
import bose.ankush.weatherify.presentation.theme.WeatherifyTheme
import bose.ankush.commonui.web.InAppWebView
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel
import javax.inject.Inject

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private val viewModel: MainViewModel by viewModels()

    // Koin-managed: owns payment state and Razorpay flow
    private val paymentViewModel: PaymentViewModel by koinViewModel()

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

                // Toast anchor state — auto-measures bottom bar height
                val toastAnchorState = rememberToastAnchorState()

                // NotificationToast state
                var toastVisible by remember { mutableStateOf(false) }
                var toastMessage by remember { mutableStateOf("") }
                var toastTitle by remember { mutableStateOf("") }
                var toastType by remember { mutableStateOf(ToastType.ERROR) }

                fun showToast(
                    message: String,
                    title: String = "Error",
                    type: ToastType = ToastType.ERROR
                ) {
                    toastMessage = message
                    toastTitle = title
                    toastType = type
                    toastVisible = true
                }

                // Handle authentication state changes
                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.Error -> {
                            showToast((authState as AuthState.Error).message.asString(this@MainActivity))
                            viewModel.resetAuthState()
                        }
                        is AuthState.Success -> {
                            showToast("Authentication successful", "Success", ToastType.SUCCESS)
                            viewModel.resetAuthState()
                        }
                        else -> Unit
                    }
                }

                // Listen for Unauthorized events
                LaunchedEffect(Unit) {
                    bose.ankush.network.auth.events.AuthEventBus.events.collect { event ->
                        if (event is bose.ankush.network.auth.events.AuthEvent.Unauthorized) {
                            showToast(
                                message = event.message.ifBlank {
                                    "You need to log in again to continue using the app for security purposes."
                                },
                                title = "Session Expired",
                                type = ToastType.WARNING
                            )
                        }
                    }
                }

                // Collect checkout params from PaymentViewModel and launch Razorpay
                LaunchedEffect(Unit) {
                    paymentViewModel.checkoutParams.collect { params ->
                        try {
                            Checkout.preload(applicationContext)
                            razorpayCheckout = Checkout()
                            razorpayCheckout?.setKeyID(params.keyId)
                            val options = JSONObject().apply {
                                put("name", params.name)
                                put("description", params.description)
                                put("order_id", params.orderId)
                                put("currency", params.currency)
                                put("amount", params.amount)
                                val prefill = JSONObject().apply {
                                    params.email?.let { put("email", it) }
                                    params.contact?.let { put("contact", it) }
                                }
                                put("prefill", prefill)
                            }
                            razorpayCheckout?.open(this@MainActivity, options)
                        } catch (e: Exception) {
                            paymentViewModel.onPaymentFailed(
                                e.message ?: "Unable to open payment checkout"
                            )
                            Checkout.clearUserData(context)
                            razorpayCheckout = null
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
                                // Update whether notification permission is permanently declined
                                val isPermanentlyDeclined = !shouldShowRequestPermissionRationale(ACCESS_NOTIFICATION)
                                viewModel.updateNotificationPermissionPermanentlyDeclined(isPermanentlyDeclined)
                            }
                            AppNavigation(viewModel, paymentViewModel, toastAnchorState)
                        }
                        else -> {
                            // State for web view
                            var currentWebUrl by remember { mutableStateOf<String?>(null) }

                            // Show web view if a URL is selected
                            if (currentWebUrl != null) {
                                InAppWebView(
                                    url = currentWebUrl!!,
                                    onClose = { currentWebUrl = null }
                                )
                            } else {
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
                                    onWebUrlClick = { url -> currentWebUrl = url },
                                    isLoading = authState is AuthState.Loading
                                )
                            }
                        }
                    }
                    NotificationToast(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        message = toastMessage,
                        title = toastTitle,
                        type = toastType,
                        isVisible = toastVisible,
                        onDismiss = { toastVisible = false },
                        anchorState = toastAnchorState
                    )
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
            val isPermanentlyDeclined = !shouldShowRequestPermissionRationale(permission)
            val textProvider = when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION -> FineLocationPermissionTextProvider()
                Manifest.permission.ACCESS_COARSE_LOCATION -> CoarseLocationPermissionTextProvider()
                else -> return@forEach
            }

            // Consumer owns back-press: exit the app when permanently declined
            BackHandler(enabled = isPermanentlyDeclined) { finish() }

            PermissionAlertDialog(
                descriptionText = textProvider.getDescription(isPermanentlyDeclined),
                isPermanentlyDeclined = isPermanentlyDeclined,
                onPositiveAction = if (isPermanentlyDeclined) {
                    { context.openAppSystemSettings() }
                } else {
                    {
                        viewModel.dismissDialog()
                        locationPermissionsResultLauncher.launch(PERMISSIONS_TO_REQUEST)
                    }
                },
                onNegativeAction = { finish() },
                positiveButtonLabel = if (isPermanentlyDeclined) "Grant Permission" else "OK",
                negativeButtonLabel = "Exit",
            )
        }

        // Launch initial permission request if missing and queue is empty (first-launch scenario)
        LaunchedEffect(Unit) {
            if (permissionQueue.isEmpty() && !locationClient.hasLocationPermission()) {
                locationPermissionsResultLauncher.launch(PERMISSIONS_TO_REQUEST)
            }
        }

        // Re-launch only when rationale should be shown (not permanently declined)
        LaunchedEffect(permissionQueue.size) {
            val hasRationalePermission =
                permissionQueue.any { shouldShowRequestPermissionRationale(it) }
            if (permissionQueue.isNotEmpty() && hasRationalePermission) {
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
        // If user granted a permission via system Settings and returned, clear it from the queue
        val granted = viewModel.permissionDialogQueue.filter { permission ->
            checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (granted.isNotEmpty()) {
            viewModel.removeGrantedPermissions(granted)
        }
        // If GPS was disabled and user returned from location settings, retry location fetch
        if (viewModel.uiState.value.isGpsDisabled && locationClient.hasLocationPermission()) {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val isLocationAvailable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (isLocationAvailable) {
                viewModel.fetchAndSaveLocationCoordinates()
            }
        }
    }

    /**
     * Razorpay payment success callback — delegates to [PaymentViewModel].
     */
    override fun onPaymentSuccess(razorpayPaymentID: String?, paymentData: PaymentData?) {
        val orderId = paymentData?.orderId.orEmpty()
        val paymentId = paymentData?.paymentId ?: razorpayPaymentID.orEmpty()
        val signature = paymentData?.signature.orEmpty()
        if (orderId.isNotBlank() && paymentId.isNotBlank() && signature.isNotBlank()) {
            paymentViewModel.verifyPayment(orderId, paymentId, signature)
        } else {
            paymentViewModel.onPaymentFailed("Payment succeeded but missing data")
        }
        razorpayCheckout = null
    }

    /**
     * Razorpay payment error callback — delegates to [PaymentViewModel].
     */
    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val message = response ?: "Payment failed with code $code"
        paymentViewModel.onPaymentFailed(message)
        razorpayCheckout = null
    }

    override fun onDestroy() {
        super.onDestroy()
        razorpayCheckout = null
    }
}
