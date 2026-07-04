package bose.ankush.weatherify.presentation.navigation

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import bose.ankush.auth.presentation.AuthIntent
import bose.ankush.auth.presentation.AuthState
import bose.ankush.auth.presentation.AuthViewModel
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.settings.SettingsScreen
import bose.ankush.commonui.settings.SettingsScreenState
import bose.ankush.commonui.settings.SettingsScreenStrings
import bose.ankush.finder.presentation.savedlocations.SavedLocationsFinderRoute
import bose.ankush.finder.presentation.savedlocations.SavedLocationsStrings
import bose.ankush.language.presentation.LanguageScreen
import bose.ankush.payment.presentation.PaymentIntent
import bose.ankush.payment.presentation.PaymentStage
import bose.ankush.payment.presentation.PaymentViewModel
import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.LocaleConfigMapper
import bose.ankush.weatherify.base.common.Extension.hasNotificationPermission
import bose.ankush.weatherify.base.common.Extension.isDeviceSDKAndroid13OrAbove
import bose.ankush.weatherify.base.common.Extension.openAppLocaleSettings
import bose.ankush.weatherify.presentation.MainViewModel
import bose.ankush.weatherify.presentation.SettingsEvent
import bose.ankush.weatherify.presentation.SettingsViewModel
import bose.ankush.weatherify.presentation.cities.CitiesListScreen
import bose.ankush.weatherify.presentation.home.HomeScreen
import bose.ankush.weatherify.presentation.strings.rememberLanguageScreenStrings

@SuppressLint("NewApi")
@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    toastAnchorState: ToastAnchorState? = null,
) {
    val navigationState = rememberAppNavigationState()
    val navigator = remember { AppNavigator(navigationState) }

    NavDisplay(
        entries =
            navigationState.toEntries(
                entryProvider {
                    entry<HomeRoute> { HomeScreen(viewModel, navigator, toastAnchorState) }
                    entry<CitiesListRoute> { CitiesListScreen(navigator) }
                    entry<SavedLocationsRoute> {
                        SavedLocationsEntry(
                            viewModel,
                            navigator,
                            toastAnchorState,
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsEntry(
                            viewModel,
                            authViewModel,
                            paymentViewModel,
                            navigator,
                            toastAnchorState,
                        )
                    }
                    entry<LanguageRoute> { route ->
                        LanguageScreen(
                            languages = route.languages.toTypedArray(),
                            strings = rememberLanguageScreenStrings(),
                        ) { navigator.goBack() }
                    }
                },
            ),
        onBack = navigator::goBack,
    )
}

@Composable
private fun SavedLocationsEntry(
    viewModel: MainViewModel,
    navigator: AppNavigator,
    toastAnchorState: ToastAnchorState?,
) {
    SavedLocationsFinderRoute(
        onLocationSelected = { lat, lon, name -> viewModel.setDefaultLocation(lat, lon, name) },
        strings = rememberSavedLocationsStrings(),
        bottomBar = {
            AppBottomBar(rememberSaveable { mutableStateOf(true) }, navigator, toastAnchorState)
        },
    )
}

@SuppressLint("NewApi")
@Composable
private fun SettingsEntry(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    navigator: AppNavigator,
    toastAnchorState: ToastAnchorState?,
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val paymentUiState by paymentViewModel.uiState.collectAsState()
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val serviceSubscriptionUiState by settingsViewModel.serviceSubscriptionViewModel.uiState.collectAsState()
    val isBottomBarVisible = rememberSaveable { mutableStateOf(true) }
    val previousPaymentStage = remember { mutableStateOf(paymentUiState.stage) }
    val languageList = rememberLanguageList()

    LaunchedEffect(paymentUiState.stage) {
        if (paymentUiState.stage == PaymentStage.Success && previousPaymentStage.value != PaymentStage.Success) {
            settingsViewModel.showPremiumActivationToast()
        }
        previousPaymentStage.value = paymentUiState.stage
    }

    SettingsScreen(
        paymentUiState = paymentUiState,
        isLoggingOut = authState is AuthState.LogoutLoading,
        isLoggedOut = authState is AuthState.LoggedOut,
        versionName = BuildConfig.VERSION_NAME,
        shouldShowNotificationItem = isDeviceSDKAndroid13OrAbove() && !context.hasNotificationPermission(),
        languageList = languageList,
        uiState = settingsUiState,
        strings = rememberSettingsStrings(),
        serviceSubscriptionBottomSheetUiState = serviceSubscriptionUiState,
        onLogout = { authViewModel.processIntent(AuthIntent.Logout) },
        onLoggedOutHandled = { authViewModel.processIntent(AuthIntent.Reset) },
        onStartPayment = { paymentViewModel.processIntent(PaymentIntent.StartPayment(it)) },
        onLoadServices = { settingsViewModel.serviceSubscriptionViewModel.loadServices() },
        onServiceSelected = { settingsViewModel.serviceSubscriptionViewModel.selectService(it) },
        onTierSelected = { settingsViewModel.serviceSubscriptionViewModel.selectPricingTier(it) },
        onBackNavAction = navigator::goBack,
        onLanguageNavAction = { list ->
            if (isDeviceSDKAndroid13OrAbove()) {
                navigator.navigate(LanguageRoute(list.toList()))
            } else {
                context.openAppLocaleSettings()
            }
        },
        onNotificationNavAction = {
            if (!context.hasNotificationPermission()) viewModel.updateNotificationPermission(true)
        },
        onStateChange = { settingsViewModel.handleScreenStateChange(it, settingsUiState) },
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
    val list =
        remember(context) {
            runCatching {
                LocaleConfigMapper.getAvailableLanguagesFromJson("countryConfig.json", context)
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

@Composable
private fun rememberSavedLocationsStrings(): SavedLocationsStrings {
    val noResultsTemplate = stringResource(R.string.place_search_no_results)
    val setAsDefaultBodyTemplate = stringResource(R.string.set_as_default_dialog_body)
    return SavedLocationsStrings(
        title = stringResource(R.string.saved_locations_title),
        premiumTitle = stringResource(R.string.saved_locations_premium_title),
        premiumDesc = stringResource(R.string.saved_locations_premium_desc),
        emptyText = stringResource(R.string.saved_locations_empty_txt),
        searchHint = stringResource(R.string.place_search_hint),
        searchDialogTitle = stringResource(R.string.place_search_dialog_title),
        noResults = { query -> noResultsTemplate.replace("%1\$s", query) },
        deleteContentDesc = stringResource(R.string.delete_icon_content),
        addContentDesc = stringResource(R.string.add_icon_content),
        cancelBtn = stringResource(R.string.cancel_btn_txt),
        saveSuccessMsg = stringResource(R.string.saved_locations_save_success),
        deleteSuccessMsg = stringResource(R.string.saved_locations_delete_success),
        setAsDefaultDialogTitle = stringResource(R.string.set_as_default_dialog_title),
        setAsDefaultDialogBody = { name -> setAsDefaultBodyTemplate.replace($$"%1$s", name) },
        setAsDefaultDialogWarning = stringResource(R.string.set_as_default_dialog_warning),
        setAsDefaultConfirmBtn = stringResource(R.string.set_as_default_confirm_btn),
    )
}

@Composable
private fun rememberSettingsStrings() =
    SettingsScreenStrings(
        profileTitle = stringResource(R.string.profile_title),
        logout = stringResource(R.string.logout_btn_txt),
        logoutConfirmation = stringResource(R.string.logout_confirmation_txt),
        confirm = stringResource(R.string.confirm_btn_txt),
        cancel = stringResource(R.string.cancel_btn_txt),
        getPremium = stringResource(R.string.premium_get_txt),
        processing = stringResource(R.string.premium_processing_txt),
        processingDescription = stringResource(R.string.premium_processing_desc_txt),
        unlockDescription = stringResource(R.string.premium_unlock_desc_txt),
        upgradeNow = stringResource(R.string.premium_upgrade_btn_txt),
        premiumActive = stringResource(R.string.premium_active_txt),
        premiumExpires = stringResource(R.string.premium_expires_txt),
        premiumActiveStatus = stringResource(R.string.premium_active_status_txt),
        notificationsTitle = stringResource(R.string.settings_notifications_txt),
        languageTitle = stringResource(R.string.settings_language_txt),
        privacyPolicy = stringResource(R.string.legal_privacy_policy_txt),
        termsOfUse = stringResource(R.string.legal_terms_of_use_txt),
        appVersion = stringResource(R.string.legal_app_version_txt),
        backButtonDesc = stringResource(R.string.back_button_content),
        arrowRightDesc = stringResource(R.string.arrow_right_icon_content),
        premiumActivatedTitle = stringResource(R.string.premium_activated_title_txt),
        premiumActivatedMessage = stringResource(R.string.premium_activated_msg_txt),
    )

private fun SettingsViewModel.handleScreenStateChange(
    newState: SettingsScreenState,
    current: SettingsScreenState,
) {
    when {
        newState.showPremiumBottomSheet != current.showPremiumBottomSheet -> {
            if (!newState.showPremiumBottomSheet) serviceSubscriptionViewModel.resetState()
            handleEvent(
                if (newState.showPremiumBottomSheet) {
                    SettingsEvent.OpenPremiumSheet
                } else {
                    SettingsEvent.ClosePremiumSheet
                },
            )
        }

        newState.showLogoutDialog != current.showLogoutDialog ->
            handleEvent(
                if (newState.showLogoutDialog) {
                    SettingsEvent.OpenLogoutDialog
                } else {
                    SettingsEvent.CloseLogoutDialog
                },
            )

        newState.showPremiumActivationToast != current.showPremiumActivationToast ->
            if (!newState.showPremiumActivationToast) handleEvent(SettingsEvent.DismissPremiumToast)

        newState.currentWebUrl != current.currentWebUrl ->
            handleEvent(
                newState.currentWebUrl?.let(SettingsEvent::OpenWebUrl)
                    ?: SettingsEvent.CloseWebView,
            )
    }
}
