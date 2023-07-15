package bose.ankush.weatherify.presentation.navigation

import androidx.annotation.StringRes
import bose.ankush.weatherify.R

sealed class Screen(val route: String, @StringRes val resourceId: Int) {

    /*Home Screens*/
    data object HomeNestedNav : Screen("home_nav", R.string.home_nested_nav)
    data object SplashScreen : Screen("splash_screen", R.string.splash_screen)
    data object HomeScreen : Screen("home_screen", R.string.home_screen)
    data object CitiesListScreen : Screen("city_list_screen", R.string.city_screen)
    data object AirQualityDetailsScreen : Screen("air_quality_details_screen", R.string.aq_screen)

    /*Running Screens*/
    data object RunNestedNav : Screen("run_nav", R.string.run_nested_nav)
    data object RunScreen : Screen("run_screen", R.string.run_screen)

    /*Account/Profile Screens*/
    data object ProfileNestedNav : Screen("profile_nav", R.string.profile_nested_nav)
    data object SettingsScreen : Screen("settings_screen", R.string.settings_screen)
    data object LanguageScreen : Screen("language_screen", R.string.account_screen)

    fun withArgs(vararg args: String?): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }

    fun withArgs(vararg args: Array<String>): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/${arg.joinToString(",")}")
            }
        }
    }
}
