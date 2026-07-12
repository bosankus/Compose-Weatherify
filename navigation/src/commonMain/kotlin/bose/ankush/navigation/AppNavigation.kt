package bose.ankush.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.entryProvider
import bose.ankush.analytics.AnalyticsEvent
import bose.ankush.analytics.AnalyticsTracker
import bose.ankush.auth.presentation.AuthIntent
import bose.ankush.auth.presentation.AuthState
import bose.ankush.auth.presentation.AuthViewModel
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.permissions.PermissionAlertDialog
import bose.ankush.finder.presentation.savedlocations.SavedLocationsFinderRoute
import bose.ankush.home.HomeLocationCoordinator
import bose.ankush.home.HomeNotificationPermissionResult
import bose.ankush.home.presentation.HomeFeatureRoute
import bose.ankush.language.presentation.LanguageScreen
import bose.ankush.language.util.LanguageCatalog
import bose.ankush.navigation.generated.resources.Res
import bose.ankush.navigation.generated.resources.exit_btn_txt
import bose.ankush.navigation.generated.resources.grant_permission_btn_txt
import bose.ankush.navigation.generated.resources.locale_config_error_txt
import bose.ankush.navigation.generated.resources.location_permission_declined_ios_txt
import bose.ankush.navigation.generated.resources.location_permission_declined_txt
import bose.ankush.navigation.generated.resources.location_permission_rationale_txt
import bose.ankush.navigation.platform.ExitAppOnBackPress
import bose.ankush.navigation.platform.ObserveAppForeground
import bose.ankush.navigation.platform.RequestLocationPermission
import bose.ankush.navigation.platform.RequestNotificationPermission
import bose.ankush.navigation.platform.rememberExitAppAction
import bose.ankush.navigation.platform.rememberPlatformPermissions
import bose.ankush.payment.presentation.PaymentIntent
import bose.ankush.payment.presentation.PaymentViewModel
import bose.ankush.settings.presentation.SettingsFeatureRoute
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    versionName: String,
    onShowToast: (String) -> Unit,
    toastAnchorState: ToastAnchorState? = null,
) {
    val navigationState = rememberAppNavigationState()
    val navigator = remember { AppNavigator(navigationState) }
    val platformPermissions = rememberPlatformPermissions()
    val lifecycleOwner = LocalLifecycleOwner.current
    val homeLocationCoordinator = koinInject<HomeLocationCoordinator>()
    val analyticsTracker = koinInject<AnalyticsTracker>()
    val coroutineScope = rememberCoroutineScope()

    var hasLocationPermission by remember { mutableStateOf(platformPermissions.hasLocationPermission()) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasNotificationPermission = platformPermissions.hasNotificationPermission()
    }

    var showNotificationPermissionRequest by remember { mutableStateOf(false) }
    var notificationPermissionResult by remember {
        mutableStateOf<HomeNotificationPermissionResult?>(null)
    }

    var isNotificationPermissionPermanentlyDeclined by remember { mutableStateOf(false) }

    var locationPermissionRequestId by remember { mutableStateOf(0) }
    var showLocationPermissionRationale by remember { mutableStateOf(false) }
    var isLocationPermissionPermanentlyDeclined by remember { mutableStateOf(false) }
    val exitApp = rememberExitAppAction()

    if (!hasLocationPermission) {
        key(locationPermissionRequestId) {
            RequestLocationPermission(
                onResult = { granted, permanentlyDeclined ->
                    hasLocationPermission = granted
                    isLocationPermissionPermanentlyDeclined = permanentlyDeclined
                    showLocationPermissionRationale = !granted
                    analyticsTracker.track(
                        AnalyticsEvent.PermissionResult("location", granted, permanentlyDeclined),
                    )
                },
            )
        }
        if (showLocationPermissionRationale) {
            PermissionAlertDialog(
                descriptionText =
                    stringResource(
                        when {
                            !isLocationPermissionPermanentlyDeclined -> Res.string.location_permission_rationale_txt
                            platformPermissions.requiresManualSettingsNavigationHint() ->
                                Res.string.location_permission_declined_ios_txt
                            else -> Res.string.location_permission_declined_txt
                        },
                    ),
                isPermanentlyDeclined = isLocationPermissionPermanentlyDeclined,
                onPositiveAction = {
                    if (isLocationPermissionPermanentlyDeclined) {
                        platformPermissions.openAppSystemSettings()
                    } else {
                        showLocationPermissionRationale = false
                        locationPermissionRequestId++
                    }
                },
                onNegativeAction = {
                    // exitApp() is a no-op on iOS (Apple's HIG forbids programmatic quitting), so
                    // the dialog must dismiss itself here rather than relying solely on the app
                    // exiting — otherwise it's stuck with no escape when permission is declined.
                    showLocationPermissionRationale = false
                    exitApp()
                },
                positiveButtonLabel =
                    if (isLocationPermissionPermanentlyDeclined) {
                        stringResource(Res.string.grant_permission_btn_txt)
                    } else {
                        "OK"
                    },
                negativeButtonLabel = stringResource(Res.string.exit_btn_txt),
            )
        }
    }

    if (showNotificationPermissionRequest) {
        RequestNotificationPermission(
            onResult = { granted, permanentlyDeclined ->
                hasNotificationPermission = granted
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

    val refreshPermissionState = {
        hasLocationPermission = platformPermissions.hasLocationPermission()
        coroutineScope.launch {
            hasNotificationPermission = platformPermissions.hasNotificationPermission()
        }
        Unit
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshPermissionState()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Backstop for platforms (iOS) whose LocalLifecycleOwner doesn't fire ON_RESUME on an
    // OS-level app foreground that isn't also a view-controller re-appear — see
    // ObserveAppForeground's kdoc.
    ObserveAppForeground(onForeground = refreshPermissionState)

    AppNavHost(
        entries =
            navigationState.toEntries(
                entryProvider {
                    entry<HomeRoute> {
                        TrackedScreen("home", "HomeScreen", analyticsTracker)
                        ExitAppOnBackPress()
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
                            requiresNotificationSettingsNavigationHint =
                                platformPermissions.requiresManualSettingsNavigationHint(),
                            onRequestNotificationPermission = {
                                showNotificationPermissionRequest = true
                            },
                            onRequestLocationPermission = {
                                locationPermissionRequestId++
                            },
                            onOpenSettings = { platformPermissions.openAppSystemSettings() },
                            onOpenLocationSettings = { platformPermissions.openLocationSettings() },
                        )
                    }
                    entry<SavedLocationsRoute> {
                        TrackedScreen("saved_locations", "SavedLocationsScreen", analyticsTracker)
                        SavedLocationsFinderRoute(
                            onLocationSelected = { lat, lon, name ->
                                coroutineScope.launch {
                                    homeLocationCoordinator.setDefaultLocation(lat, lon, name)
                                }
                            },
                            onUpgradeClick = { navigator.navigate(SettingsRoute) },
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
                        TrackedScreen("settings", "SettingsScreen", analyticsTracker)
                        SettingsEntry(
                            authViewModel,
                            paymentViewModel,
                            navigator,
                            toastAnchorState,
                            versionName = versionName,
                            onShowToast = onShowToast,
                            hasNotificationPermission = hasNotificationPermission,
                            isNotificationPermissionPermanentlyDeclined =
                            isNotificationPermissionPermanentlyDeclined,
                            onRequestNotificationPermission = {
                                showNotificationPermissionRequest = true
                            },
                        )
                    }
                    entry<LanguageRoute> { route ->
                        TrackedScreen("language", "LanguageScreen", analyticsTracker)
                        LanguageScreen(languages = route.languages) { navigator.goBack() }
                    }
                },
            ),
        onBack = navigator::goBack,
    )
}

/** Fires once per navigation-in — [LaunchedEffect] re-runs each time this entry is recomposed
 * fresh (nav3 tears down/rebuilds top-level tab entries on switch), matching Firebase's own
 * `screen_view` semantics of firing again on tab re-selection. */
@Composable
private fun TrackedScreen(
    screenName: String,
    screenClass: String,
    tracker: AnalyticsTracker,
) {
    LaunchedEffect(Unit) {
        tracker.track(AnalyticsEvent.ScreenView(screenName, screenClass))
    }
}

@Composable
private fun SettingsEntry(
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    navigator: AppNavigator,
    toastAnchorState: ToastAnchorState?,
    versionName: String,
    onShowToast: (String) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    hasNotificationPermission: Boolean,
    isNotificationPermissionPermanentlyDeclined: Boolean,
) {
    val platformPermissions = rememberPlatformPermissions()
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
        onStartPayment = { paymentViewModel.processIntent(PaymentIntent.StartPayment(it)) },
        onBackNavAction = navigator::goBack,
        onLanguageNavAction = { list ->
            if (platformPermissions.supportsPerAppLocaleSettings()) {
                navigator.navigate(LanguageRoute(list.toList()))
            } else {
                platformPermissions.openAppLocaleSettings()
            }
        },
        onNotificationNavAction = {
            when {
                isNotificationPermissionPermanentlyDeclined -> platformPermissions.openAppSystemSettings()
                !hasNotificationPermission -> onRequestNotificationPermission()
                else -> Unit
            }
        },
        onBottomBarVisibilityChange = { isBottomBarVisible.value = it },
        toastAnchorState = toastAnchorState,
        bottomBar = { AppBottomBar(isBottomBarVisible, navigator, toastAnchorState) },
    )
}

@Composable
private fun rememberLanguageList(onShowToast: (String) -> Unit): Array<String> {
    val errorMessage = stringResource(Res.string.locale_config_error_txt)
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
