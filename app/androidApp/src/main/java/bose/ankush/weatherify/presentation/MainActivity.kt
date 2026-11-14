package bose.ankush.weatherify.presentation

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.auth.presentation.AuthEffect
import bose.ankush.auth.presentation.AuthIntent
import bose.ankush.auth.presentation.AuthState
import bose.ankush.auth.presentation.AuthViewModel
import bose.ankush.auth.presentation.LoginScreen
import bose.ankush.commonui.components.NotificationToast
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.components.ToastType
import bose.ankush.commonui.components.rememberToastAnchorState
import bose.ankush.commonui.theme.WeatherifyTheme
import bose.ankush.commonui.web.InAppWebView
import bose.ankush.finder.presentation.savedlocations.SavedLocationsFinderRoute
import bose.ankush.home.HomeFirstContent
import bose.ankush.home.HomeSessionCleaner
import bose.ankush.language.util.AppEnvironment
import bose.ankush.language.util.LanguageCatalog
import bose.ankush.navigation.AppNavigation
import bose.ankush.navigation.HomeTabContentFactory
import bose.ankush.navigation.LanguageContentFactory
import bose.ankush.navigation.SavedLocationsContentFactory
import bose.ankush.navigation.SettingsContentFactory
import bose.ankush.payment.presentation.CheckoutParams
import bose.ankush.payment.presentation.PaymentEffect
import bose.ankush.payment.presentation.PaymentIntent
import bose.ankush.payment.presentation.PaymentViewModel
import bose.ankush.settings.presentation.SettingsFeatureRoute
import bose.ankush.settings.presentation.SettingsPresentation
import bose.ankush.storage.api.PremiumStorage
import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.base.common.InAppUpdateManager
import com.google.firebase.messaging.FirebaseMessaging
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject
import org.koin.android.ext.android.inject
import timber.log.Timber
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel

class MainActivity :
    AppCompatActivity(),
    PaymentResultWithDataListener {
    // Koin-managed: owns auth state and session management
    private val authViewModel: AuthViewModel by koinViewModel()

    // Koin-managed: owns payment state and Razorpay flow
    private val paymentViewModel: PaymentViewModel by koinViewModel()

    // Koin-managed: cross-feature bridges into :feature:home
    private val premiumStorage: PremiumStorage by inject()
    private val homeSessionCleaner: HomeSessionCleaner by inject()
    private val analyticsTracker: AnalyticsTracker by inject()

    private val inAppUpdates = InAppUpdateManager(this)

    // Hold a reference to the Checkout instance only during payment
    private var razorpayCheckout: Checkout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        holdSplashUntilAuthResolves(installSplashScreen())
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WeatherifyTheme(isDynamicColor = true) { AppEnvironment { AppContent() } } }
    }

    /**
     * Keeps the system splash up until the sign-in state is known and, when signed in, until Home
     * has drawn its cached forecast; then fades it out, so launch goes splash, then a finished
     * screen, with no spinner or placeholder frame in between. Capped so a stuck auth check or a
     * slow cache read can never hold the app on the splash.
     */
    private fun holdSplashUntilAuthResolves(splash: SplashScreen) {
        val startedAt = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition {
            val contentReady =
                authViewModel.isAuthInitialized.value &&
                        (!authViewModel.isLoggedIn.value || HomeFirstContent.isReady.value)
            !contentReady && SystemClock.elapsedRealtime() - startedAt < SPLASH_MAX_HOLD_MILLIS
        }
        splash.setOnExitAnimationListener { provider ->
            provider.view
                .animate()
                .alpha(0f)
                .setDuration(SPLASH_FADE_MILLIS)
                .withEndAction { provider.remove() }
                .start()
        }
    }

    @Composable
    private fun AppContent() {
        val context = LocalContext.current
        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
        val authState by authViewModel.authState.collectAsState()
        val isAuthInitialized by authViewModel.isAuthInitialized.collectAsState()
        // Tells the system when the first real screen is up, for launch-time measurement.
        ReportDrawnWhen { isAuthInitialized }
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

        ObserveAuthState(authState = authState, onShowToast = ::showToast)
        ObserveAuthEffect()
        ObservePaymentEffect(context = context)

        AppScreen(
            isAuthInitialized = isAuthInitialized,
            isLoggedIn = isLoggedIn,
            authState = authState,
            toastController =
                ToastController(
                    anchorState = toastAnchorState,
                    onShowToast = { message -> showToast(message) },
                ),
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

                is AuthState.AccountDeleted -> {
                    onShowToast(authState.message, "Account deleted", ToastType.SUCCESS)
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
                        premiumStorage.savePremiumStatus(effect.isPremium, effect.expiryMillis)

                    AuthEffect.LoggedOut -> {
                        homeSessionCleaner.clearOnLogout()
                        premiumStorage.savePremiumStatus(isPremium = false, expiryMillis = null)
                    }

                    AuthEffect.AccountDeleted -> {
                        homeSessionCleaner.clearOnLogout()
                        premiumStorage.savePremiumStatus(isPremium = false, expiryMillis = null)
                        clearAccountBoundDeviceState()
                    }
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
        toastController: ToastController,
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

                isLoggedIn -> AuthorizedContent(toastController = toastController)
                else -> UnauthorizedContent(authState = authState)
            }
            NotificationToast(
                modifier = Modifier.align(Alignment.BottomCenter),
                message = toastState.message,
                title = toastState.title,
                type = toastState.type,
                isVisible = toastState.visible,
                onDismiss = toastState.onDismiss,
                anchorState = toastController.anchorState,
            )
        }
    }

    @Composable
    private fun AuthorizedContent(toastController: ToastController) {
        val homeTabContent = remember { HomeTabContentFactoryImpl() }
        val savedLocationsContent = remember { SavedLocationsContentFactoryImpl() }
        val settingsContent = remember { SettingsContentFactoryImpl() }
        val languageContent = remember { LanguageContentFactoryImpl() }

        AppNavigation(
            authViewModel = authViewModel,
            paymentViewModel = paymentViewModel,
            versionName = BuildConfig.VERSION_NAME,
            onShowToast = toastController.onShowToast,
            toastAnchorState = toastController.anchorState,
            homeTabContent = homeTabContent,
            savedLocationsContent = savedLocationsContent,
            settingsContent = settingsContent,
            languageContent = languageContent,
        )
    }

    private inner class HomeTabContentFactoryImpl : HomeTabContentFactory {
        @Composable
        override fun Weather(
            toastAnchorState: ToastAnchorState?,
            hasLocationPermission: Boolean,
            hasNotificationPermission: Boolean,
            notificationPermissionResult: bose.ankush.home.HomeNotificationPermissionResult?,
            requiresNotificationSettingsNavigationHint: Boolean,
            onRequestNotificationPermission: () -> Unit,
            onRequestLocationPermission: () -> Unit,
            onOpenNotificationSettings: () -> Unit,
            onOpenLocationSettings: () -> Unit,
        ) {
            bose.ankush.home.presentation.HomeFeatureRoute(
                toastAnchorState = toastAnchorState,
                hasLocationPermission = hasLocationPermission,
                hasNotificationPermission = hasNotificationPermission,
                notificationPermissionResult = notificationPermissionResult,
                requiresNotificationSettingsNavigationHint = requiresNotificationSettingsNavigationHint,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestLocationPermission = onRequestLocationPermission,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenLocationSettings = onOpenLocationSettings,
            )
        }

        @Composable
        override fun Places(
            onLocationSelected: (Double, Double, String) -> Unit,
            onUpgradeClick: () -> Unit,
        ) {
            SavedLocationsFinderRoute(
                onLocationSelected = onLocationSelected,
                onUpgradeClick = onUpgradeClick,
            )
        }

        @Composable
        override fun Hub(
            bottomInset: androidx.compose.ui.unit.Dp,
            onCoverChange: (close: (() -> Unit)?) -> Unit,
            authViewModel: AuthViewModel,
            paymentViewModel: PaymentViewModel,
            navigator: bose.ankush.navigation.AppNavigator,
            toastAnchorState: ToastAnchorState?,
            versionName: String,
            onShowToast: (String) -> Unit,
            hasNotificationPermission: Boolean,
            isNotificationPermissionPermanentlyDeclined: Boolean,
            onRequestNotificationPermission: () -> Unit,
        ) {
            val platformPermissions = bose.ankush.navigation.platform.rememberPlatformPermissions()
            val authState by authViewModel.authState.collectAsState()
            val paymentUiState by paymentViewModel.uiState.collectAsState()
            val isBottomBarVisible = rememberSaveable { mutableStateOf(true) }
            val languageList = rememberLanguageList(onShowToast)

            SettingsFeatureRoute(
                paymentUiState = paymentUiState,
                isLoggingOut = authState is AuthState.LogoutLoading,
                isLoggedOut = authState is AuthState.LoggedOut,
                versionName = versionName,
                shouldShowNotificationItem =
                    platformPermissions.requiresRuntimeNotificationPermission() &&
                            !hasNotificationPermission,
                languageList = languageList,
                onLogout = { authViewModel.processIntent(AuthIntent.Logout) },
                onLoggedOutHandled = { authViewModel.processIntent(AuthIntent.Reset) },
                onStartPayment = {
                    paymentViewModel.processIntent(
                        PaymentIntent.StartPayment(
                            it
                        )
                    )
                },
                onBackNavAction = navigator::goBack,
                onLanguageNavAction = { list ->
                    if (platformPermissions.supportsPerAppLocaleSettings()) {
                        navigator.navigate(bose.ankush.routing.LanguageRoute(list.toList()))
                    } else {
                        platformPermissions.openAppLocaleSettings()
                    }
                },
                onNotificationNavAction = {
                    when {
                        isNotificationPermissionPermanentlyDeclined -> platformPermissions.openNotificationSettings()
                        !hasNotificationPermission -> onRequestNotificationPermission()
                        else -> Unit
                    }
                },
                onBottomBarVisibilityChange = { isBottomBarVisible.value = it },
                onRefreshPremium = { authViewModel.processIntent(AuthIntent.RefreshToken) },
                toastAnchorState = toastAnchorState,
                bottomBar = {
                    if (SettingsPresentation.Page(
                            bottomInset,
                            onCoverChange
                        ) == SettingsPresentation.Screen
                    ) {
                        bose.ankush.navigation.AppBottomBar(
                            isBottomBarVisible,
                            navigator,
                            toastAnchorState
                        )
                    }
                },
                presentation = SettingsPresentation.Page(
                    bottomInset,
                    onCoverChange
                ),
            )
        }
    }

    private class SavedLocationsContentFactoryImpl : SavedLocationsContentFactory {
        @Composable
        override fun SavedLocations(
            onLocationSelected: (Double, Double, String) -> Unit,
            onUpgradeClick: () -> Unit,
            bottomBar: @Composable () -> Unit,
        ) {
            SavedLocationsFinderRoute(
                onLocationSelected = onLocationSelected,
                onUpgradeClick = onUpgradeClick,
                bottomBar = bottomBar,
            )
        }
    }

    private inner class SettingsContentFactoryImpl : SettingsContentFactory {
        @Composable
        override fun Settings(
            authViewModel: AuthViewModel,
            paymentViewModel: PaymentViewModel,
            navigator: bose.ankush.navigation.AppNavigator,
            toastAnchorState: ToastAnchorState?,
            versionName: String,
            onShowToast: (String) -> Unit,
            hasNotificationPermission: Boolean,
            isNotificationPermissionPermanentlyDeclined: Boolean,
            onRequestNotificationPermission: () -> Unit,
            presentation: SettingsPresentation,
            bottomBar: @Composable (() -> Unit)?,
        ) {
            val platformPermissions = bose.ankush.navigation.platform.rememberPlatformPermissions()
            val authState by authViewModel.authState.collectAsState()
            val paymentUiState by paymentViewModel.uiState.collectAsState()
            val isBottomBarVisible = rememberSaveable { mutableStateOf(true) }
            val languageList = rememberLanguageList(onShowToast)

            SettingsFeatureRoute(
                paymentUiState = paymentUiState,
                isLoggingOut = authState is AuthState.LogoutLoading,
                isLoggedOut = authState is AuthState.LoggedOut,
                versionName = versionName,
                shouldShowNotificationItem =
                    platformPermissions.requiresRuntimeNotificationPermission() &&
                            !hasNotificationPermission,
                languageList = languageList,
                onLogout = { authViewModel.processIntent(AuthIntent.Logout) },
                onLoggedOutHandled = { authViewModel.processIntent(AuthIntent.Reset) },
                onStartPayment = {
                    paymentViewModel.processIntent(
                        PaymentIntent.StartPayment(
                            it
                        )
                    )
                },
                onBackNavAction = navigator::goBack,
                onLanguageNavAction = { list ->
                    if (platformPermissions.supportsPerAppLocaleSettings()) {
                        navigator.navigate(bose.ankush.routing.LanguageRoute(list.toList()))
                    } else {
                        platformPermissions.openAppLocaleSettings()
                    }
                },
                onNotificationNavAction = {
                    when {
                        isNotificationPermissionPermanentlyDeclined -> platformPermissions.openNotificationSettings()
                        !hasNotificationPermission -> onRequestNotificationPermission()
                        else -> Unit
                    }
                },
                onBottomBarVisibilityChange = { isBottomBarVisible.value = it },
                onRefreshPremium = { authViewModel.processIntent(AuthIntent.RefreshToken) },
                toastAnchorState = toastAnchorState,
                bottomBar = bottomBar ?: {
                    if (presentation == SettingsPresentation.Screen) {
                        bose.ankush.navigation.AppBottomBar(
                            isBottomBarVisible,
                            navigator,
                            toastAnchorState
                        )
                    }
                },
                presentation = presentation,
            )
        }
    }

    private class LanguageContentFactoryImpl : LanguageContentFactory {
        @Composable
        override fun LanguageScreen(languages: List<String>, onBack: () -> Unit) {
            bose.ankush.language.presentation.LanguageScreen(
                languages = languages,
                navAction = onBack,
            )
        }
    }

    @Composable
    private fun rememberLanguageList(onShowToast: (String) -> Unit): Array<String> {
        val errorMessage = "Failed to load languages"
        var list by remember { mutableStateOf(emptyArray<String>()) }
        LaunchedEffect(Unit) {
            list =
                runCatching {
                    LanguageCatalog.getAvailableLanguages().toTypedArray()
                }.getOrElse {
                    onShowToast(errorMessage)
                    emptyArray()
                }
        }
        return list
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
            LaunchedEffect(Unit) {
                analyticsTracker.track(AnalyticsEvent.ScreenView("login", "LoginScreen"))
            }
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

    /**
     * What a deleted account leaves on the device beyond the session: the push token the server
     * stored at registration (rotated so it can't reach this device again) and Razorpay's
     * remembered checkout details.
     */
    private fun clearAccountBoundDeviceState() {
        FirebaseMessaging
            .getInstance()
            .deleteToken()
            .addOnFailureListener {
                Timber.w(
                    it,
                    "Couldn't rotate the FCM token after account deletion"
                )
            }
        Checkout.clearUserData(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        inAppUpdates.checkForUpdate()
        authViewModel.processIntent(AuthIntent.RefreshOnForeground)
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
        releaseRazorpayDismissCallback()
    }

    /**
     * `Checkout.open(activity, …)` parks a callback capturing the Activity in the static
     * `OpinionatedSoln.dismissCallback` and the SDK never clears it, so every Activity that
     * opened checkout leaks (~16 MB) until the next payment overwrites it. There is no public
     * API for this; the SDK's consumer rules keep `com.razorpay.**`, so the name is stable.
     */
    private fun releaseRazorpayDismissCallback() {
        runCatching {
            Class
                .forName("com.razorpay.OpinionatedSoln")
                .getDeclaredField("dismissCallback")
                .apply { isAccessible = true }
                .set(null, null)
        }.onFailure { Timber.w(it, "Couldn't release Razorpay dismissCallback") }
    }
}

private data class ToastDisplayState(
    val visible: Boolean,
    val message: String,
    val title: String,
    val type: ToastType,
    val onDismiss: () -> Unit,
)

private data class ToastController(
    val anchorState: ToastAnchorState,
    val onShowToast: (String) -> Unit,
)

private const val SPLASH_MAX_HOLD_MILLIS = 1_500L
private const val SPLASH_FADE_MILLIS = 220L
