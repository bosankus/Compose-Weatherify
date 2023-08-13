package bose.ankush.weatherify.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import bose.ankush.language.presentation.LanguageScreen
import bose.ankush.weatherify.base.common.Extension.isDeviceSDKAndroid13OrAbove
import bose.ankush.weatherify.base.common.Extension.openAppLocaleSettings
import bose.ankush.weatherify.presentation.cities.CitiesListScreen
import bose.ankush.weatherify.presentation.home.AirQualityDetailsScreen
import bose.ankush.weatherify.presentation.home.HomeScreen
import bose.ankush.weatherify.presentation.home.WeatherViewModel
import bose.ankush.weatherify.presentation.run.RunScreen
import bose.ankush.weatherify.presentation.settings.SettingsScreen

const val LANGUAGE_ARGUMENT_KEY = "country_config"

@SuppressLint("NewApi")
@ExperimentalAnimationApi
@Composable
fun AppNavigation() {
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
            ) { entry ->
                val viewModel =
                    entry.sharedViewModel<WeatherViewModel>(navController = navController)
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
            composable(
                route = Screen.AirQualityDetailsScreen.route,
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
                val viewModel =
                    entry.sharedViewModel<WeatherViewModel>(navController = navController)
                AirQualityDetailsScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }

        /*Running Screens*/
        navigation(
            startDestination = Screen.RunScreen.route,
            route = Screen.RunNestedNav.route
        ) {
            composable(
                route = Screen.RunScreen.route,
            ) {
                RunScreen(navController = navController)
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
                    onNavAction = { navController.popBackStack() },
                    onLanguageNavAction = {
                        if (isDeviceSDKAndroid13OrAbove()) {
                            navController.navigate(Screen.LanguageScreen.withArgs(it))
                        } else {
                            context.openAppLocaleSettings()
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

@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(navController: NavController): T {
    val navGraphRoute = destination.parent?.route ?: return hiltViewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return hiltViewModel(parentEntry)
}
