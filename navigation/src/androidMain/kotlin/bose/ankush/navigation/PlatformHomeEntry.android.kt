package bose.ankush.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import bose.ankush.home.presentation.screen.HomeScreenLinks
import bose.ankush.home.presentation.screen.HomeScreenRoute

@Composable
actual fun PlatformHomeEntry(
    weather: @Composable () -> Unit,
    places: @Composable () -> Unit,
    hub: @Composable (bottomInset: Dp, onCoverChange: (close: (() -> Unit)?) -> Unit) -> Unit,
) {
    HomeScreenRoute(
        links =
            HomeScreenLinks(
                weather = weather,
                places = places,
                hub = hub,
            ),
    )
}
