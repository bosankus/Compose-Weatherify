package bose.ankush.weatherify.navigation

sealed class Screen(val route: String) {
    object HomeScreen : Screen("home_screen")
    object CitiesListScreen : Screen("city_list_screen")
}