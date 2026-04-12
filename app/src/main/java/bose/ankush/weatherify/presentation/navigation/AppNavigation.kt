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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import bose.ankush.commonui.settings.SettingsScreen
import bose.ankush.commonui.settings.SettingsScreenStrings
import bose.ankush.commonui.components.PremiumBottomSheetStrings
import bose.ankush.language.presentation.LanguageScreen
import bose.ankush.commonui.components.ToastAnchorState
import bose.ankush.payment.presentation.PaymentViewModel
import bose.ankush.weatherify.BuildConfig
import bose.ankush.weatherify.R
import bose.ankush.weatherify.base.LocaleConfigMapper
import bose.ankush.weatherify.base.common.Extension.hasNotificationPermission
import bose.ankush.weatherify.base.common.Extension.isDeviceSDKAndroid13OrAbove
import bose.ankush.weatherify.base.common.Extension.openAppLocaleSettings
import bose.ankush.weatherify.presentation.AuthState
import bose.ankush.weatherify.presentation.MainViewModel
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
                SettingsScreen(
                    paymentUiState = paymentUiState,
                    isLoggingOut = authState is AuthState.LogoutLoading,
                    isLoggedOut = authState is AuthState.LoggedOut,
                    versionName = BuildConfig.VERSION_NAME,
                    shouldShowNotificationItem = isDeviceSDKAndroid13OrAbove() && !context.hasNotificationPermission(),
                    languageList = languageList,
                    onLogout = { viewModel.logout() },
                    onLoggedOutHandled = { viewModel.resetAuthState() },
                    onStartPayment = { paymentViewModel.startPayment() },
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
                        arrowRightDesc = stringResource(R.string.arrow_right_icon_content)
                    ),
                    premiumStrings = PremiumBottomSheetStrings(
                        title = stringResource(R.string.premium_title_txt),
                        features = listOf(
                            stringResource(R.string.premium_feature_1_txt),
                            stringResource(R.string.premium_feature_2_txt),
                            stringResource(R.string.premium_feature_3_txt),
                            stringResource(R.string.premium_feature_4_txt)
                        ),
                        priceText = stringResource(R.string.premium_price_txt),
                        trialText = stringResource(R.string.premium_trial_txt),
                        subscribeButtonText = stringResource(R.string.premium_subscribe_btn_txt),
                        startingText = stringResource(R.string.premium_starting_txt),
                        cancelText = stringResource(R.string.premium_no_thanks_txt)
                    ),
                    toastAnchorState = toastAnchorState,
                    bottomBar = {
                        AppBottomBar(
                            isVisible = rememberSaveable { mutableStateOf(true) },
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
