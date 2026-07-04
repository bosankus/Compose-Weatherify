package bose.ankush.weatherify.presentation

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import bose.ankush.auth.presentation.AuthEffect
import bose.ankush.auth.presentation.AuthIntent
import bose.ankush.auth.presentation.AuthState
import bose.ankush.auth.presentation.AuthViewModel
import bose.ankush.auth.presentation.LoginScreen
import bose.ankush.commonui.components.NotificationToast
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.components.ToastType
import bose.ankush.commonui.components.rememberToastAnchorState
import bose.ankush.commonui.permissions.PermissionAlertDialog
import bose.ankush.commonui.web.InAppWebView
import bose.ankush.payment.presentation.CheckoutParams
import bose.ankush.payment.presentation.PaymentEffect
import bose.ankush.payment.presentation.PaymentIntent
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
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import org.koin.compose.KoinContext
import javax.inject.Inject
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@AndroidEntryPoint
class MainActivity :
    AppCompatActivity(),
    PaymentResultWithDataListener {
    private val viewModel: MainViewModel by viewModels()

    // Koin-managed: owns auth state and session management
    private val authViewModel: AuthViewModel by koinViewModel()

    // Koin-managed: owns payment state and Razorpay flow
    private val paymentViewModel: PaymentViewModel by koinViewModel()

    // Android-managed: owns location state
    @Inject
    lateinit var locationClient: LocationClient

    // Hold a reference to the Checkout instance only during payment
    private var razorpayCheckout: Checkout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startInAppUpdate(this)
        setContent {
            KoinContext {
                WeatherifyTheme {
                    AppContent()
                }
            }
        }
    }

    @Composable
    private fun AppContent() {
        val context = LocalContext.current
        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
        val authState by authViewModel.authState.collectAsState()
        val isAuthInitialized by authViewModel.isAuthInitialized.collectAsState()
        val toastAnchorState = rememberToastAnchorState()
        var toastVisible by remember { mutableStateOf(false) }
        var toastMessage by remember { mutableStateOf("") }
        var toastTitle by remember { mutableStateOf("") }
        var toastType by remember { mutableStateOf(ToastType.ERROR) }

        fun showToast(
            message: String,
            title: String = "Error",
            type: ToastType = ToastType.ERROR,
        ) {
            toastMessage = message
            toastTitle = title
            toastType = type
            toastVisible = true
        }

        ObserveAuthState(authState = authState, context = context, onShowToast = ::showToast)
        ObserveAuthEffect()
        ObservePaymentEffect(context = context)

        AppScreen(
            isAuthInitialized = isAuthInitialized,
            isLoggedIn = isLoggedIn,
            authState = authState,
            toastAnchorState = toastAnchorState,
            toastState =
                ToastDisplayState(
                    visible = toastVisible,
                    message = toastMessage,
                    title = toastTitle,
                    type = toastType,
                    onDismiss = { toastVisible = false },
                ),
        )
    }

    @Composable
    private fun ObserveAuthState(
        authState: AuthState,
        context: Context,
        onShowToast: (String, String, ToastType) -> Unit,
    ) {
        LaunchedEffect(authState) {
            when (authState) {
                is AuthState.Error -> {
                    onShowToast(authState.message, "Error", ToastType.ERROR)
                    authViewModel.processIntent(AuthIntent.Reset)
                }
                is AuthState.Success -> {
                    onShowToast("Authentication successful", "Success", ToastType.SUCCESS)
                    authViewModel.processIntent(AuthIntent.Reset)
                }
                is AuthState.SessionExpired -> {
                    onShowToast(authState.message, "Session Expired", ToastType.WARNING)
                    authViewModel.processIntent(AuthIntent.Reset)
                }
                else -> Unit
            }
        }
    }

    @Composable
    private fun ObserveAuthEffect() {
        LaunchedEffect(Unit) {
            authViewModel.effect.collect { effect ->
                when (effect) {
                    is AuthEffect.PremiumStatusChanged ->
                        viewModel.updatePremiumStatus(effect.isPremium, effect.expiryMillis)
                    AuthEffect.LoggedOut ->
                        viewModel.handleLoggedOut()
                }
            }
        }
    }

    @Composable
    private fun ObservePaymentEffect(context: Context) {
        LaunchedEffect(Unit) {
            paymentViewModel.effect.collect { effect ->
                when (effect) {
                    is PaymentEffect.LaunchCheckout -> launchCheckout(context, effect.params)
                }
            }
        }
    }

    @Composable
    private fun AppScreen(
        isAuthInitialized: Boolean,
        isLoggedIn: Boolean,
        authState: AuthState,
        toastAnchorState: ToastAnchorState,
        toastState: ToastDisplayState,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ),
        ) {
            when {
                !isAuthInitialized -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }

                isLoggedIn -> AuthorizedContent(toastAnchorState = toastAnchorState)
                else -> UnauthorizedContent(authState = authState)
            }
            NotificationToast(
                modifier = Modifier.align(Alignment.BottomCenter),
                message = toastState.message,
                title = toastState.title,
                type = toastState.type,
                isVisible = toastState.visible,
                onDismiss = toastState.onDismiss,
                anchorState = toastAnchorState,
            )
        }
    }

    @Composable
    private fun AuthorizedContent(toastAnchorState: ToastAnchorState) {
        val context = LocalContext.current
        val launchNotificationPermissionState =
            viewModel.launchNotificationPermission.collectAsState()
        LaunchedEffect(true) {
            if (locationClient.hasLocationPermission()) {
                viewModel.fetchAndSaveLocationCoordinates()
            }
        }
        if (!locationClient.hasLocationPermission()) {
            RequestLocationPermission(context)
        }
        if (launchNotificationPermissionState.value) {
            RequestNotificationPermission(context)
        }
        LaunchedEffect(launchNotificationPermissionState.value) {
            viewModel.updateShowNotificationBannerState(!context.hasNotificationPermission())
        }
        AppNavigation(viewModel, authViewModel, paymentViewModel, toastAnchorState)
    }

    @Composable
    private fun UnauthorizedContent(authState: AuthState) {
        var currentWebUrl by remember { mutableStateOf<String?>(null) }
        if (currentWebUrl != null) {
            InAppWebView(
                url = currentWebUrl!!,
                onClose = { currentWebUrl = null },
            )
        } else {
            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.processIntent(AuthIntent.Login(email, password))
                },
                onRegisterClick = { email, password ->
                    authViewModel.processIntent(AuthIntent.Register(email, password))
                },
                onWebUrlClick = { url -> currentWebUrl = url },
                isLoading = authState is AuthState.Loading,
            )
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
                            isGranted = permissionMap[permission] == true,
                        )
                    }
                },
            )

        permissionQueue.reversed().forEach { permission ->
            val isPermanentlyDeclined = !shouldShowRequestPermissionRationale(permission)
            val textProvider =
                when (permission) {
                    Manifest.permission.ACCESS_FINE_LOCATION -> FineLocationPermissionTextProvider()
                    Manifest.permission.ACCESS_COARSE_LOCATION -> CoarseLocationPermissionTextProvider()
                    else -> return@forEach
                }

            // Consumer owns back-press: exit the app when permanently declined
            BackHandler(enabled = isPermanentlyDeclined) { finish() }

            PermissionAlertDialog(
                descriptionText = textProvider.getDescription(isPermanentlyDeclined),
                isPermanentlyDeclined = isPermanentlyDeclined,
                onPositiveAction =
                    if (isPermanentlyDeclined) {
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

    @Composable
    fun RequestNotificationPermission(context: Context) {
        val notificationPermissionResultLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    viewModel.updateShowNotificationBannerState(!isGranted)
                    if (isGranted) {
                        Toast
                            .makeText(
                                context,
                                "Notification permission granted",
                                Toast.LENGTH_SHORT,
                            ).show()
                    } else {
                        val isPermanentlyDeclined =
                            !shouldShowRequestPermissionRationale(ACCESS_NOTIFICATION)
                        viewModel.updateNotificationPermissionPermanentlyDeclined(
                            isPermanentlyDeclined,
                        )
                    }
                },
            )
        LaunchedEffect(Unit) {
            notificationPermissionResultLauncher.launch(ACCESS_NOTIFICATION)
        }
    }

    override fun onResume() {
        super.onResume()
        startInAppUpdate(this)
        authViewModel.processIntent(AuthIntent.RefreshToken)
        // If user granted a permission via system Settings and returned, clear it from the queue
        val granted =
            viewModel.permissionDialogQueue.filter { permission ->
                checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        if (granted.isNotEmpty()) {
            viewModel.removeGrantedPermissions(granted)
        }
        // If GPS was disabled and user returned from location settings, retry location fetch
        if (viewModel.uiState.value.isGpsDisabled && locationClient.hasLocationPermission()) {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val isLocationAvailable =
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (isLocationAvailable) {
                viewModel.fetchAndSaveLocationCoordinates()
            }
        }
    }

    private fun launchCheckout(
        context: Context,
        params: CheckoutParams,
    ) {
        try {
            Checkout.preload(applicationContext)
            razorpayCheckout = Checkout()
            razorpayCheckout?.setKeyID(params.keyId)
            val options =
                JSONObject().apply {
                    put("name", params.name)
                    put("description", params.description)
                    put("order_id", params.orderId)
                    put("currency", params.currency)
                    put("amount", params.amount)
                    val prefill =
                        JSONObject().apply {
                            params.email?.let { put("email", it) }
                            params.contact?.let { put("contact", it) }
                        }
                    put("prefill", prefill)
                }
            razorpayCheckout?.open(this@MainActivity, options)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            paymentViewModel.processIntent(
                PaymentIntent.PaymentFailed(
                    e.message ?: "Unable to open payment checkout",
                ),
            )
            Checkout.clearUserData(context)
            razorpayCheckout = null
        }
    }

    /**
     * Razorpay payment success callback — delegates to [PaymentViewModel].
     */
    override fun onPaymentSuccess(
        razorpayPaymentID: String?,
        paymentData: PaymentData?,
    ) {
        val orderId = paymentData?.orderId.orEmpty()
        val paymentId = paymentData?.paymentId ?: razorpayPaymentID.orEmpty()
        val signature = paymentData?.signature.orEmpty()
        if (orderId.isNotBlank() && paymentId.isNotBlank() && signature.isNotBlank()) {
            paymentViewModel.processIntent(
                PaymentIntent.VerifyPayment(
                    orderId,
                    paymentId,
                    signature,
                ),
            )
        } else {
            paymentViewModel.processIntent(
                PaymentIntent.PaymentFailed("Payment succeeded but missing data"),
            )
        }
        razorpayCheckout = null
    }

    /**
     * Razorpay payment error callback — delegates to [PaymentViewModel].
     */
    override fun onPaymentError(
        code: Int,
        response: String?,
        paymentData: PaymentData?,
    ) {
        val message = response ?: "Payment failed with code $code"
        paymentViewModel.processIntent(PaymentIntent.PaymentFailed(message))
        razorpayCheckout = null
    }

    override fun onDestroy() {
        super.onDestroy()
        razorpayCheckout = null
    }
}

private data class ToastDisplayState(
    val visible: Boolean,
    val message: String,
    val title: String,
    val type: ToastType,
    val onDismiss: () -> Unit,
)
