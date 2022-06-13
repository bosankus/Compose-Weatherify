package bose.ankush.weatherify.navigation

sealed class Screen(val route: String) {
    object HomeScreen : Screen("home_screen")
    object CitiesListScreen : Screen("city_list_screen")

    fun withArgs(vararg args: String?): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}