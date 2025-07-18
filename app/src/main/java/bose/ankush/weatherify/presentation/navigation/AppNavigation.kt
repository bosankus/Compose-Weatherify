package bose.ankush.weatherify.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import bose.ankush.language.presentation.LanguageScreen
import bose.ankush.weatherify.base.common.Extension.hasNotificationPermission
import bose.ankush.weatherify.base.common.Extension.isDeviceSDKAndroid13OrAbove
import bose.ankush.weatherify.base.common.Extension.openAppLocaleSettings
import bose.ankush.weatherify.presentation.MainViewModel
import bose.ankush.weatherify.presentation.cities.CitiesListScreen
import bose.ankush.weatherify.presentation.home.HomeScreen
import bose.ankush.weatherify.presentation.settings.SettingsScreen

const val LANGUAGE_ARGUMENT_KEY = "country_config"

@SuppressLint("NewApi")
@ExperimentalAnimationApi
@Composable
fun AppNavigation(viewModel: MainViewModel) {
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
                    navController = navController
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
                SettingsScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onLanguageNavAction = {
                        if (isDeviceSDKAndroid13OrAbove()) {
                            navController.navigate(Screen.LanguageScreen.withArgs(it))
                        } else {
                            context.openAppLocaleSettings()
                        }
                    },
                    onNotificationNavAction = {
                        if (!context.hasNotificationPermission()) {
                            viewModel.updateNotificationPermission(launchState = true)
                        }
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
