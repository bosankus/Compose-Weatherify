package bose.ankush.weatherify.presentation.navigation

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.commonui.locations.SavedLocationsScreen
import bose.ankush.commonui.locations.SavedLocationsStrings
import bose.ankush.commonui.settings.SettingsScreen
import bose.ankush.commonui.settings.SettingsScreenStrings
import bose.ankush.language.presentation.LanguageScreen
import bose.ankush.payment.presentation.PaymentViewModel
import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.LocaleConfigMapper
import bose.ankush.weatherify.base.common.Extension.hasNotificationPermission
import bose.ankush.weatherify.base.common.Extension.isDeviceSDKAndroid13OrAbove
import bose.ankush.weatherify.base.common.Extension.openAppLocaleSettings
import bose.ankush.weatherify.presentation.AuthState
import bose.ankush.weatherify.presentation.MainViewModel
import bose.ankush.weatherify.presentation.SettingsEvent
import bose.ankush.weatherify.presentation.SettingsViewModel
import bose.ankush.weatherify.presentation.cities.CitiesListScreen
import bose.ankush.weatherify.presentation.home.HomeScreen

const val LANGUAGE_ARGUMENT_KEY = "country_config"

@SuppressLint("NewApi")
@ExperimentalAnimationApi
@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    paymentViewModel: PaymentViewModel,
    toastAnchorState: ToastAnchorState? = null,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = Screen.HomeNestedNav.route
    ) {
        /*Home Screens*/
        navigation(
            startDestination = Screen.HomeScreen.route,
            route = Screen.HomeNestedNav.route
        ) {
            composable(
                route = Screen.HomeScreen.route,
            ) {
                HomeScreen(
                    viewModel = viewModel,
                    navController = navController,
                    toastAnchorState = toastAnchorState
                )
            }
            composable(
                route = Screen.CitiesListScreen.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(500)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(500)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(500)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(500)
                    )
                },
            ) {
                CitiesListScreen(navController = navController)
            }
        }

        /*Saved Locations (Premium Feature)*/
        navigation(
            startDestination = Screen.SavedLocationsScreen.route,
            route = Screen.SavedLocationsNestedNav.route
        ) {
            composable(route = Screen.SavedLocationsScreen.route) {
                val locationsState = viewModel.savedLocationsState.collectAsState().value
                val searchState = viewModel.placeSearchState.collectAsState().value

                SavedLocationsScreen(
                    locationsState = locationsState,
                    searchState = searchState,
                    onQueryChanged = { query -> viewModel.onPlaceSearchQueryChanged(query) },
                    onClearSearch = { viewModel.clearPlaceSearch() },
                    onSaveLocation = { name, lat, lon -> viewModel.saveLocation(name, lat, lon) },
                    onDeleteLocation = { id -> viewModel.deleteLocation(id) },
                    onMessageShown = { viewModel.clearLocationMessage() },
                    strings = SavedLocationsStrings(
                        title = stringResource(R.string.saved_locations_title),
                        premiumTitle = stringResource(R.string.saved_locations_premium_title),
                        premiumDesc = stringResource(R.string.saved_locations_premium_desc),
                        emptyText = stringResource(R.string.saved_locations_empty_txt),
                        searchHint = stringResource(R.string.place_search_hint),
                        searchDialogTitle = stringResource(R.string.place_search_dialog_title),
                        noResults = stringResource(R.string.place_search_no_results),
                        deleteContentDesc = stringResource(R.string.delete_icon_content),
                        addContentDesc = stringResource(R.string.add_icon_content),
                        cancelBtn = stringResource(R.string.cancel_btn_txt),
                        saveSuccessMsg = stringResource(R.string.saved_locations_save_success),
                        deleteSuccessMsg = stringResource(R.string.saved_locations_delete_success)
                    ),
                    bottomBar = {
                        AppBottomBar(
                            isVisible = rememberSaveable { mutableStateOf(true) },
                            navController = navController,
                            toastAnchorState = toastAnchorState
                        )
                    }
                )
            }
        }

        /*Account/Profile Screens*/
        navigation(
            startDestination = Screen.SettingsScreen.route,
            route = Screen.ProfileNestedNav.route
        ) {
            composable(
                route = Screen.SettingsScreen.route,
            ) {
                val authState = viewModel.authState.collectAsState().value
                val paymentUiState = paymentViewModel.uiState.collectAsState().value
                val localeErrorMessage = stringResource(R.string.locale_config_error_txt)
                val showLocaleError = remember { mutableStateOf(false) }
                val languageList = remember(context) {
                    try {
                        LocaleConfigMapper.getAvailableLanguagesFromJson(
                            jsonFile = "countryConfig.json",
                            context = context
                        )
                    } catch (_: Exception) {
                        showLocaleError.value = true
                        emptyArray()
                    }
                }

                LaunchedEffect(showLocaleError.value) {
                    if (showLocaleError.value) {
                        Toast.makeText(context, localeErrorMessage, Toast.LENGTH_SHORT).show()
                        showLocaleError.value = false
                    }
                }

                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val settingsUiState = settingsViewModel.uiState.collectAsState().value
                val serviceSubscriptionBottomSheetUiState =
                    settingsViewModel.serviceSubscriptionViewModel.uiState.collectAsState().value
                val isBottomBarVisible = rememberSaveable { mutableStateOf(true) }

                LaunchedEffect(paymentUiState.stage) {
                    if (paymentUiState.stage == bose.ankush.payment.presentation.PaymentStage.Success) {
                        settingsViewModel.showPremiumActivationToast()
                    }
                }

                SettingsScreen(
                    paymentUiState = paymentUiState,
                    isLoggingOut = authState is AuthState.LogoutLoading,
                    isLoggedOut = authState is AuthState.LoggedOut,
                    versionName = BuildConfig.VERSION_NAME,
                    shouldShowNotificationItem = isDeviceSDKAndroid13OrAbove() && !context.hasNotificationPermission(),
                    languageList = languageList,
                    uiState = settingsUiState,
                    strings = SettingsScreenStrings(
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
                        premiumActivatedMessage = stringResource(R.string.premium_activated_msg_txt)
                    ),
                    serviceSubscriptionBottomSheetUiState = serviceSubscriptionBottomSheetUiState,
                    onLogout = { viewModel.logout() },
                    onLoggedOutHandled = { viewModel.resetAuthState() },
                    onStartPayment = { amountPaise -> paymentViewModel.startPayment(amountPaise) },
                    onLoadServices = { settingsViewModel.serviceSubscriptionViewModel.loadServices() },
                    onServiceSelected = { service ->
                        settingsViewModel.serviceSubscriptionViewModel.selectService(
                            service
                        )
                    },
                    onTierSelected = { tier ->
                        settingsViewModel.serviceSubscriptionViewModel.selectPricingTier(
                            tier
                        )
                    },
                    onBackNavAction = { navController.popBackStack() },
                    onLanguageNavAction = { list ->
                        if (isDeviceSDKAndroid13OrAbove()) {
                            navController.navigate(Screen.LanguageScreen.withArgs(list))
                        } else {
                            context.openAppLocaleSettings()
                        }
                    },
                    onNotificationNavAction = {
                        if (!context.hasNotificationPermission()) {
                            viewModel.updateNotificationPermission(launchState = true)
                        }
                    },
                    onStateChange = { newState ->
                        when {
                            newState.showPremiumBottomSheet != settingsUiState.showPremiumBottomSheet -> {
                                if (!newState.showPremiumBottomSheet) {
                                    settingsViewModel.serviceSubscriptionViewModel.resetState()
                                }
                                settingsViewModel.handleEvent(
                                    if (newState.showPremiumBottomSheet) SettingsEvent.OpenPremiumSheet else SettingsEvent.ClosePremiumSheet
                                )
                            }

                            newState.showLogoutDialog != settingsUiState.showLogoutDialog ->
                                settingsViewModel.handleEvent(
                                    if (newState.showLogoutDialog) SettingsEvent.OpenLogoutDialog else SettingsEvent.CloseLogoutDialog
                                )

                            newState.showPremiumActivationToast != settingsUiState.showPremiumActivationToast ->
                                settingsViewModel.handleEvent(SettingsEvent.DismissPremiumToast)

                            newState.currentWebUrl != settingsUiState.currentWebUrl -> {
                                val url = newState.currentWebUrl
                                if (url != null) {
                                    settingsViewModel.handleEvent(SettingsEvent.OpenWebUrl(url))
                                } else {
                                    settingsViewModel.handleEvent(SettingsEvent.CloseWebView)
                                }
                            }
                        }
                    },
                    onBottomBarVisibilityChange = { isVisible ->
                        isBottomBarVisible.value = isVisible
                    },
                    toastAnchorState = toastAnchorState,
                    bottomBar = {
                        AppBottomBar(
                            isVisible = isBottomBarVisible,
                            navController = navController,
                            toastAnchorState = toastAnchorState
                        )
                    }
                )
            }
            composable(
                route = Screen.LanguageScreen.route + "/{$LANGUAGE_ARGUMENT_KEY}",
                arguments = listOf(navArgument(LANGUAGE_ARGUMENT_KEY) {
                    type = StringListType()
                    nullable = false
                }),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                }
            ) { entry ->
                entry.arguments?.let {
                    it.getStringArray(LANGUAGE_ARGUMENT_KEY)?.let { listOfString ->
                        LanguageScreen(
                            languages = listOfString,
                            navAction = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
