package bose.ankush.weatherify.presentation.navigation

sealed class Screen(val route: String) {
    object HomeScreen : Screen("home_screen")
    object CitiesListScreen : Screen("city_list_screen")
    object AirQualityDetailsScreen : Screen("air_quality_details_screen")
    object LanguageScreen : Screen("language_screen")
    fun withArgs(vararg args: String?): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }

    fun withArgs(vararg args: Double?): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/${arg}")
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