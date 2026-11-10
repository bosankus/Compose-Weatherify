package bose.ankush.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import bose.ankush.analytics.ErrorReporter
import bose.ankush.auth.presentation.AuthViewModel
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.permissions.PermissionAlertDialog
import bose.ankush.home.HomeLocationCoordinator
import bose.ankush.home.HomeNotificationPermissionResult
import bose.ankush.navigation.generated.resources.Res
import bose.ankush.navigation.generated.resources.exit_btn_txt
import bose.ankush.navigation.generated.resources.grant_permission_btn_txt
import bose.ankush.navigation.generated.resources.location_permission_declined_ios_txt
import bose.ankush.navigation.generated.resources.location_permission_declined_txt
import bose.ankush.navigation.generated.resources.location_permission_rationale_txt
import bose.ankush.navigation.platform.ExitAppOnBackPress
import bose.ankush.navigation.platform.ObserveAppForeground
import bose.ankush.navigation.platform.RequestLocationPermission
import bose.ankush.navigation.platform.RequestNotificationPermission
import bose.ankush.navigation.platform.rememberExitAppAction
import bose.ankush.navigation.platform.rememberPlatformPermissions
import bose.ankush.payment.presentation.PaymentViewModel
import bose.ankush.routing.HomeRoute
import bose.ankush.routing.LanguageRoute
import bose.ankush.routing.SavedLocationsRoute
import bose.ankush.routing.SettingsRoute
import bose.ankush.settings.presentation.SettingsPresentation
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Factory for the Home tab content (three sub-tabs: weather, places, hub).
 * Provided by the host app to keep navigation module decoupled from feature implementations.
 */
interface HomeTabContentFactory {
    @Composable
    fun Weather(
        toastAnchorState: ToastAnchorState?,
        hasLocationPermission: Boolean,
        hasNotificationPermission: Boolean,
        notificationPermissionResult: HomeNotificationPermissionResult?,
        requiresNotificationSettingsNavigationHint: Boolean,
        onRequestNotificationPermission: () -> Unit,
        onRequestLocationPermission: () -> Unit,
        onOpenNotificationSettings: () -> Unit,
        onOpenLocationSettings: () -> Unit,
    )

    @Composable
    fun Places(
        onLocationSelected: (Double, Double, String) -> Unit,
        onUpgradeClick: () -> Unit,
    )

    @Composable
    fun Hub(
        bottomInset: androidx.compose.ui.unit.Dp,
        onCoverChange: ((() -> Unit)?) -> Unit,
        authViewModel: AuthViewModel,
        paymentViewModel: PaymentViewModel,
        navigator: AppNavigator,
        toastAnchorState: ToastAnchorState?,
        versionName: String,
        onShowToast: (String) -> Unit,
        hasNotificationPermission: Boolean,
        isNotificationPermissionPermanentlyDeclined: Boolean,
        onRequestNotificationPermission: () -> Unit,
    )
}

/**
 * Factory for the Saved Locations tab content.
 */
interface SavedLocationsContentFactory {
    @Composable
    fun SavedLocations(
        onLocationSelected: (Double, Double, String) -> Unit,
        onUpgradeClick: () -> Unit,
        bottomBar: @Composable () -> Unit,
    )
}

/**
 * Factory for the Settings tab content.
 */
interface SettingsContentFactory {
    @Composable
    fun Settings(
        authViewModel: AuthViewModel,
        paymentViewModel: PaymentViewModel,
        navigator: AppNavigator,
        toastAnchorState: ToastAnchorState?,
        versionName: String,
        onShowToast: (String) -> Unit,
        hasNotificationPermission: Boolean,
        isNotificationPermissionPermanentlyDeclined: Boolean,
        onRequestNotificationPermission: () -> Unit,
        presentation: SettingsPresentation = SettingsPresentation.Screen,
        bottomBar: @Composable (() -> Unit)? = null,
    )
}

/**
 * Factory for the Language screen content.
 */
interface LanguageContentFactory {
    @Composable
    fun LanguageScreen(languages: List<String>, onBack: () -> Unit)
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    versionName: String,
    onShowToast: (String) -> Unit,
    toastAnchorState: ToastAnchorState? = null,
    // Feature content factories provided by the host app
    homeTabContent: HomeTabContentFactory,
    savedLocationsContent: SavedLocationsContentFactory,
    settingsContent: SettingsContentFactory,
    languageContent: LanguageContentFactory,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val platformPermissions = rememberPlatformPermissions()
    val navigationState = rememberAppNavigationState()
    val exitApp = rememberExitAppAction()

    val homeLocationCoordinator = koinInject<HomeLocationCoordinator>()
    val analyticsTracker = koinInject<AnalyticsTracker>()
    val errorReporter = koinInject<ErrorReporter>()

    val navigator = remember { AppNavigator(navigationState) }
    var hasLocationPermission by remember { mutableStateOf(platformPermissions.hasLocationPermission()) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var showNotificationPermissionRequest by remember { mutableStateOf(false) }
    var notificationPermissionResult by remember {
        mutableStateOf<HomeNotificationPermissionResult?>(null)
    }
    var isNotificationPermissionPermanentlyDeclined by remember { mutableStateOf(false) }
    var locationPermissionRequestId by remember { mutableStateOf(0) }
    var showLocationPermissionRationale by remember { mutableStateOf(false) }
    var isLocationPermissionPermanentlyDeclined by remember { mutableStateOf(false) }

    val refreshPermissionState = {
        hasLocationPermission = platformPermissions.hasLocationPermission()
        coroutineScope.launch {
            hasNotificationPermission = platformPermissions.hasNotificationPermission()
        }
        Unit
    }

    LaunchedEffect(Unit) {
        hasNotificationPermission = platformPermissions.hasNotificationPermission()
    }

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

    ObserveAppForeground(onForeground = refreshPermissionState)

    AppNavHost(
        entries =
            navigationState.toEntries(
                entryProvider {
                    entry<HomeRoute> {
                        TrackedScreen("home", "HomeScreen", analyticsTracker, errorReporter)
                        ExitAppOnBackPress()
                        PlatformHomeEntry(
                            weather = {
                                homeTabContent.Weather(
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
                                    onOpenNotificationSettings = { platformPermissions.openNotificationSettings() },
                                    onOpenLocationSettings = { platformPermissions.openLocationSettings() },
                                )
                            },
                            places = {
                                homeTabContent.Places(
                                    onLocationSelected = { lat, lon, name ->
                                        coroutineScope.launch {
                                            homeLocationCoordinator.setDefaultLocation(
                                                lat,
                                                lon,
                                                name
                                            )
                                        }
                                    },
                                    onUpgradeClick = { navigator.navigate(SettingsRoute) },
                                )
                            },
                            hub = { bottomInset, onCoverChange ->
                                homeTabContent.Hub(
                                    bottomInset = bottomInset,
                                    onCoverChange = onCoverChange,
                                    authViewModel = authViewModel,
                                    paymentViewModel = paymentViewModel,
                                    navigator = navigator,
                                    toastAnchorState = null,
                                    versionName = versionName,
                                    onShowToast = onShowToast,
                                    hasNotificationPermission = hasNotificationPermission,
                                    isNotificationPermissionPermanentlyDeclined =
                                        isNotificationPermissionPermanentlyDeclined,
                                    onRequestNotificationPermission = {
                                        showNotificationPermissionRequest = true
                                    },
                                )
                            },
                        )
                    }
                    entry<SavedLocationsRoute> {
                        TrackedScreen(
                            "saved_locations",
                            "SavedLocationsScreen",
                            analyticsTracker,
                            errorReporter,
                        )
                        savedLocationsContent.SavedLocations(
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
                        TrackedScreen("settings", "SettingsScreen", analyticsTracker, errorReporter)
                        settingsContent.Settings(
                            authViewModel = authViewModel,
                            paymentViewModel = paymentViewModel,
                            navigator = navigator,
                            toastAnchorState = toastAnchorState,
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
                        TrackedScreen("language", "LanguageScreen", analyticsTracker, errorReporter)
                        languageContent.LanguageScreen(
                            languages = route.languages,
                            onBack = navigator::goBack,
                        )
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
    errorReporter: ErrorReporter,
) {
    LaunchedEffect(Unit) {
        tracker.track(AnalyticsEvent.ScreenView(screenName, screenClass))
        errorReporter.log("screen: $screenName")
    }
}