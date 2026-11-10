package bose.ankush.home.presentation.screen

import androidx.compose.runtime.Composable

/**
 * What the host app plugs into the home shell. Home, saved places and the hub all live in the
 * home pager: the Map tab opens the places page, and the Hub tab, the account mark and
 * "upgrade" open [hub], the host's Settings drawn as a page. [places] is the standalone saved
 * locations screen; the home screen no longer renders it, and the app keeps that route
 * registered for other entry points.
 */
class HomeScreenLinks(
    val weather: @Composable () -> Unit,
    val places: @Composable () -> Unit,
    val hub: HubPageContent,
)
