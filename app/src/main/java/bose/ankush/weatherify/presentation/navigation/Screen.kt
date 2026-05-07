package bose.ankush.weatherify.presentation.navigation

import androidx.annotation.StringRes
import bose.ankush.weatherify.R

sealed class Screen(
    val route: String,
    @StringRes val resourceId: Int,
) {
    // Home Screens
    data object HomeNestedNav : Screen("home_nav", R.string.home_nested_nav)

    data object HomeScreen : Screen("home_screen", R.string.home_screen)

    data object CitiesListScreen : Screen("city_list_screen", R.string.city_screen)

    // Saved Locations (Premium)
    data object SavedLocationsNestedNav :
        Screen("saved_locations_nav", R.string.saved_locations_nested_nav)

    data object SavedLocationsScreen :
        Screen("saved_locations_screen", R.string.saved_locations_screen)

    // Account/Profile Screens
    data object ProfileNestedNav : Screen("profile_nav", R.string.profile_nested_nav)

    data object SettingsScreen : Screen("settings_screen", R.string.settings_screen)

    data object LanguageScreen : Screen("language_screen", R.string.account_screen)

    fun withArgs(vararg args: String?): String =
        buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }

    fun withArgs(vararg args: Array<String>): String =
        buildString {
            append(route)
            args.forEach { arg ->
                append("/${arg.joinToString(",")}")
            }
        }
}
