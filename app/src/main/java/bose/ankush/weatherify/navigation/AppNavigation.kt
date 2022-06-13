package bose.ankush.weatherify.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import bose.ankush.weatherify.common.DEFAULT_CITY_NAME
import bose.ankush.weatherify.presentation.cities.CitiesListScreen
import bose.ankush.weatherify.presentation.home.HomeScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

const val HOME_ARGUMENT_KEY = "city_name"

@ExperimentalAnimationApi
@Composable
fun AppNavigation() {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = Screen.HomeScreen.route + "/{$HOME_ARGUMENT_KEY}"
    ) {
        composable(
            route = Screen.HomeScreen.route + "/{$HOME_ARGUMENT_KEY}", // optional argument url
            arguments = listOf(navArgument(HOME_ARGUMENT_KEY) {
                type = NavType.StringType
                defaultValue = DEFAULT_CITY_NAME
                nullable = true
            }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentScope.SlideDirection.Up,
                    animationSpec = tween(500)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentScope.SlideDirection.Up,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentScope.SlideDirection.Down,
                    animationSpec = tween(500)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentScope.SlideDirection.Down,
                    animationSpec = tween(500)
                )
            },
        ) { backStackEntry ->
            HomeScreen(
                navController = navController,
                cityName = backStackEntry.arguments?.getString(HOME_ARGUMENT_KEY) ?: DEFAULT_CITY_NAME
            )
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
            arguments = listOf(
                navArgument("city") {
                    type = NavType.StringType
                    defaultValue = DEFAULT_CITY_NAME
                    nullable = false
                }
            )
        ) {
            CitiesListScreen(navController = navController)
        }
    }
}