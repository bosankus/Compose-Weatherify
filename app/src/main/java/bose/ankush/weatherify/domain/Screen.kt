package bose.ankush.weatherify.domain

sealed class Screen(val route: String) {
    object HomeFragmentScreen: Screen("home_fragment_screen")
    object DetailsFragmentScreen: Screen("details_fragment_screen")
}
