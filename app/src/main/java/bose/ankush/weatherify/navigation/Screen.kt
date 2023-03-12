package bose.ankush.weatherify.navigation

sealed class Screen(val route: String) {
    object HomeScreen : Screen("home_screen")
    object CitiesListScreen : Screen("city_list_screen")
    object AirQualityDetailsScreen : Screen("aiq_quality_details_screen")

    fun withArgs(vararg args: String?): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }

    fun withArgs(vararg args: Pair<String?, String?>): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/lat=${arg.first}/lan=${arg.second}")
            }
        }
    }
}
