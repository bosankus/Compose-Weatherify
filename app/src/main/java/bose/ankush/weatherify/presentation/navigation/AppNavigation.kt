package bose.ankush.weatherify.presentation.navigation

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import bose.ankush.auth.presentation.AuthIntent
import bose.ankush.auth.presentation.AuthState
import bose.ankush.auth.presentation.AuthViewModel
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.finder.presentation.savedlocations.SavedLocationsFinderRoute
import bose.ankush.home.HomeLocationCoordinator
import bose.ankush.home.HomeNotificationPermissionResult
import bose.ankush.home.presentation.HomeFeatureRoute
import bose.ankush.language.presentation.LanguageScreen
import bose.ankush.language.util.LanguageCatalog
import bose.ankush.payment.presentation.PaymentIntent
import bose.ankush.payment.presentation.PaymentViewModel
import bose.ankush.settings.presentation.SettingsFeatureRoute
import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.common.ACCESS_NOTIFICATION
import bose.ankush.weatherify.base.common.Extension.hasLocationPermission
import bose.ankush.weatherify.base.common.Extension.hasNotificationPermission
import bose.ankush.weatherify.base.common.Extension.isDeviceSDKAndroid13OrAbove
import bose.ankush.weatherify.base.common.Extension.openAppLocaleSettings
import bose.ankush.weatherify.base.common.Extension.openAppSystemSettings
import bose.ankush.weatherify.base.common.Extension.openLocationSettings
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@SuppressLint("NewApi")
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    toastAnchorState: ToastAnchorState? = null,
) {
    val navigationState = rememberAppNavigationState()
    val navigator = remember { AppNavigator(navigationState) }
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val homeLocationCoordinator = koinInject<HomeLocationCoordinator>()
    val coroutineScope = rememberCoroutineScope()

    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var hasNotificationPermission by remember { mutableStateOf(context.hasNotificationPermission()) }

    var showNotificationPermissionRequest by remember { mutableStateOf(false) }
    var notificationPermissionResult by remember {
        mutableStateOf<HomeNotificationPermissionResult?>(null)
    }

    var isNotificationPermissionPermanentlyDeclined by remember { mutableStateOf(false) }

    if (showNotificationPermissionRequest) {
        RequestNotificationPermissionForHome(
            onResult = { granted, permanentlyDeclined ->
                isNotificationPermissionPermanentlyDeclined = permanentlyDeclined
                notificationPermissionResult =
                    HomeNotificationPermissionResult(
                        isGranted = granted,
                        isPermanentlyDeclined = permanentlyDeclined,
                    )
                showNotificationPermissionRequest = false
            },
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasLocationPermission = context.hasLocationPermission()
                    hasNotificationPermission = context.hasNotificationPermission()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavDisplay(
        entries =
            navigationState.toEntries(
                entryProvider {
                    entry<HomeRoute> {
                        BackHandler { activity?.finish() }
                        HomeFeatureRoute(
                            bottomBar = {
                                AppBottomBar(
                                    rememberSaveable { mutableStateOf(true) },
                                    navigator,
                                    toastAnchorState,
                                )
                            },
                            toastAnchorState = toastAnchorState,
                            hasLocationPermission = hasLocationPermission,
                            hasNotificationPermission = hasNotificationPermission,
                            notificationPermissionResult = notificationPermissionResult,
                            onRequestNotificationPermission = {
                                showNotificationPermissionRequest = true
                            },
                            onOpenSettings = { context.openAppSystemSettings() },
                            onOpenLocationSettings = { context.openLocationSettings() },
                        )
                    }
                    entry<SavedLocationsRoute> {
                        SavedLocationsFinderRoute(
                            onLocationSelected = { lat, lon, name ->
                                coroutineScope.launch {
                                    homeLocationCoordinator.setDefaultLocation(lat, lon, name)
                                }
                            },
                            bottomBar = {
                                AppBottomBar(
                                    rememberSaveable { mutableStateOf(true) },
                                    navigator,
                                    toastAnchorState,
                                )
                            },
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsEntry(
                            authViewModel,
                            paymentViewModel,
                            navigator,
                            toastAnchorState,
                            isNotificationPermissionPermanentlyDeclined =
                            isNotificationPermissionPermanentlyDeclined,
                            onRequestNotificationPermission = {
                                showNotificationPermissionRequest = true
                            },
                        )
                    }
                    entry<LanguageRoute> { route ->
                        LanguageScreen(languages = route.languages) { navigator.goBack() }
                    }
                },
            ),
        onBack = navigator::goBack,
    )
}

/** Shared by Home's notification banner and the Settings screen's notification nav item — both
 * just need the platform permission dialog launched; the result is optional to consume. */
@Composable
private fun RequestNotificationPermissionForHome(
    onResult: (isGranted: Boolean, isPermanentlyDeclined: Boolean) -> Unit,
) {
    val activity = LocalActivity.current
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                val isPermanentlyDeclined =
                    !isGranted &&
                        activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            ACCESS_NOTIFICATION,
                        )
                onResult(isGranted, isPermanentlyDeclined)
            },
        )
    LaunchedEffect(Unit) {
        launcher.launch(ACCESS_NOTIFICATION)
    }
}

@SuppressLint("NewApi")
@Composable
private fun SettingsEntry(
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    navigator: AppNavigator,
    toastAnchorState: ToastAnchorState?,
    onRequestNotificationPermission: () -> Unit,
    isNotificationPermissionPermanentlyDeclined: Boolean,
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val paymentUiState by paymentViewModel.uiState.collectAsState()
    val isBottomBarVisible = rememberSaveable { mutableStateOf(true) }
    val languageList = rememberLanguageList()

    SettingsFeatureRoute(
        paymentUiState = paymentUiState,
        isLoggingOut = authState is AuthState.LogoutLoading,
        isLoggedOut = authState is AuthState.LoggedOut,
        versionName = BuildConfig.VERSION_NAME,
        shouldShowNotificationItem = isDeviceSDKAndroid13OrAbove() && !context.hasNotificationPermission(),
        languageList = languageList,
        onLogout = { authViewModel.processIntent(AuthIntent.Logout) },
        onLoggedOutHandled = { authViewModel.processIntent(AuthIntent.Reset) },
        onStartPayment = { paymentViewModel.processIntent(PaymentIntent.StartPayment(it)) },
        onBackNavAction = navigator::goBack,
        onLanguageNavAction = { list ->
            if (isDeviceSDKAndroid13OrAbove()) {
                navigator.navigate(LanguageRoute(list.toList()))
            } else {
                context.openAppLocaleSettings()
            }
        },
        onNotificationNavAction = {
            when {
                isNotificationPermissionPermanentlyDeclined -> context.openAppSystemSettings()
                !context.hasNotificationPermission() -> onRequestNotificationPermission()
            }
        },
        onBottomBarVisibilityChange = { isBottomBarVisible.value = it },
        toastAnchorState = toastAnchorState,
        bottomBar = { AppBottomBar(isBottomBarVisible, navigator, toastAnchorState) },
    )
}

@Composable
private fun rememberLanguageList(): Array<String> {
    val context = LocalContext.current
    val showError = remember { mutableStateOf(false) }
    val errorMessage = stringResource(R.string.locale_config_error_txt)
    var list by remember { mutableStateOf(emptyArray<String>()) }
    LaunchedEffect(Unit) {
        list =
            runCatching {
                LanguageCatalog.getAvailableLanguages().toTypedArray()
            }.getOrElse {
                showError.value = true
                emptyArray()
            }
    }
    LaunchedEffect(showError.value) {
        if (showError.value) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            showError.value = false
        }
    }
    return list
}
