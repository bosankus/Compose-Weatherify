package bose.ankush.weatherify.presentation.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import bose.ankush.language.presentation.LanguageScreen
import bose.ankush.weatherify.presentation.air_quality.AirQualityDetailsScreen
import bose.ankush.weatherify.presentation.cities.CitiesListScreen
import bose.ankush.weatherify.presentation.home.HomeScreen
import bose.ankush.weatherify.presentation.home.HomeViewModel
import bose.ankush.weatherify.presentation.run.RunScreen
import bose.ankush.weatherify.presentation.settings.SettingsScreen
import bose.ankush.weatherify.presentation.splash.SplashScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

const val LANGUAGE_ARGUMENT_KEY = "country_config"

@ExperimentalAnimationApi
@Composable
fun AppNavigation() {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = Screen.HomeNestedNav.route
    ) {
        /*Home Screens*/
        navigation(
            startDestination = Screen.SplashScreen.route,
            route = Screen.HomeNestedNav.route
        ) {
            composable(
                route = Screen.SplashScreen.route
            ) { entry ->
                val viewModel = entry.sharedViewModel<HomeViewModel>(navController = navController)
                SplashScreen(
                    viewModel = viewModel,
                    navAction = {
                        navController.popBackStack()
                        navController.navigate(Screen.HomeScreen.route)
                    })
            }
            composable(
                route = Screen.HomeScreen.route,
            ) {
                HomeScreen(navController = navController)
            }
            composable(
                route = Screen.CitiesListScreen.route,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentScope.SlideDirection.Down,
                        animationSpec = tween(500)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentScope.SlideDirection.Down,
                        animationSpec = tween(500)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentScope.SlideDirection.Up,
                        animationSpec = tween(500)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentScope.SlideDirection.Up,
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
                        towards = AnimatedContentScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                }
            ) { entry ->
                val viewModel = entry.sharedViewModel<HomeViewModel>(navController = navController)
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
                    onLanguageNavAction = { navController.navigate(Screen.LanguageScreen.withArgs(it)) }
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
                        towards = AnimatedContentScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentScope.SlideDirection.Right,
                        animationSpec = tween(500)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentScope.SlideDirection.Right,
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
