package bose.ankush.home.presentation.screen

import androidx.compose.runtime.Composable

/**
 * Where each tab goes. Home and saved places stay on this shell: the Map tab opens the
 * places page of the home pager. [places] is the standalone saved locations screen; the home
 * screen no longer renders it, and the app keeps that route registered for other entry points.
 */
class HomeScreenLinks(
    val weather: @Composable () -> Unit,
    val places: @Composable () -> Unit,
    val onOpenHub: () -> Unit,
)
