package bose.ankush.navigation

import androidx.compose.runtime.Composable
import bose.ankush.home.presentation.screen.HomeScreenLinks
import bose.ankush.home.presentation.screen.HomeScreenRoute

@Composable
actual fun PlatformHomeEntry(
    weather: @Composable () -> Unit,
    places: @Composable () -> Unit,
    onOpenHub: () -> Unit,
) {
    HomeScreenRoute(
        links =
            HomeScreenLinks(
                weather = weather,
                places = places,
                onOpenHub = onOpenHub,
            ),
    )
}
