package bose.ankush.weatherify.presentation.navigation

import android.os.Bundle
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import bose.ankush.language.presentation.LANGUAGE_ARGUMENT_KEY
import bose.ankush.language.presentation.LanguageScreen
import bose.ankush.weatherify.common.DEFAULT_CITY_NAME
import bose.ankush.weatherify.presentation.air_quality.AirQualityDetailsScreen
import bose.ankush.weatherify.presentation.cities.CitiesListScreen
import bose.ankush.weatherify.presentation.home.HomeScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

const val HOME_ARGUMENT_KEY = "city_name"
const val AQ_LAT_KEY = "location_latitude"
const val AQ_LON_KEY = "location_longitude"

@ExperimentalAnimationApi
@Composable
fun AppNavigation() {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = Screen.HomeScreen.route + "/{$HOME_ARGUMENT_KEY}"
    ) {
        composable(
            route = Screen.HomeScreen.route + "/{$HOME_ARGUMENT_KEY}",
            arguments = listOf(navArgument(HOME_ARGUMENT_KEY) {
                type = NavType.StringType
                defaultValue = DEFAULT_CITY_NAME
                nullable = true
            }
            ),

            ) { entry ->
            HomeScreen(
                navController = navController,
                cityName = entry.arguments?.getString(HOME_ARGUMENT_KEY)
                    ?: DEFAULT_CITY_NAME
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
        ) {
            CitiesListScreen(navController = navController)
        }
        composable(
            route = Screen.AirQualityDetailsScreen.route + "/{$AQ_LAT_KEY}/{$AQ_LON_KEY}",
            arguments = listOf(
                navArgument(AQ_LAT_KEY) {
                    type = DoubleNavType()
                    nullable = false
                },
                navArgument(AQ_LON_KEY) {
                    type = DoubleNavType()
                    nullable = false
                }
            ),
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
            },

            ) { entry ->
            entry.arguments?.let {
                AirQualityDetailsScreen(
                    lat = it.getDouble(AQ_LAT_KEY),
                    lon = it.getDouble(AQ_LON_KEY),
                    navController = navController
                )
            }
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
                        navController = navController
                    )
                }
            }
        }
    }
}

class DoubleNavType : NavType<Double>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): Double = bundle.getDouble(key)

    override fun parseValue(value: String): Double = value.toDouble()

    override fun put(bundle: Bundle, key: String, value: Double) {
        bundle.putDouble(key, value)
    }
}

class StringListType : NavType<List<String>>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): List<String> {
        val stringArray = bundle.getStringArray(key)
        return stringArray?.toList() ?: emptyList()
    }

    override fun parseValue(value: String): List<String> {
        return value.split(",").map { it.trim() }
    }

    override fun put(bundle: Bundle, key: String, value: List<String>) {
        bundle.putStringArray(key, value.toTypedArray())
    }
}