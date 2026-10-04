package bose.ankush.navigation

import androidx.compose.runtime.Composable
import bose.ankush.home.presentation.wander.WanderHomeLinks
import bose.ankush.home.presentation.wander.WanderHomeRoute

@Composable
actual fun PlatformHomeEntry(
    weather: @Composable () -> Unit,
    places: @Composable () -> Unit,
    onOpenHub: () -> Unit,
    onOpenTravel: () -> Unit,
) {
    WanderHomeRoute(
        links =
            WanderHomeLinks(
                weather = weather,
                places = places,
                onOpenHub = onOpenHub,
                onOpenTravel = onOpenTravel,
            ),
    )
}
