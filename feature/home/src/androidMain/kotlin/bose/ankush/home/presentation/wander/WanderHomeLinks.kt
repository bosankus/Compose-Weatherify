package bose.ankush.home.presentation.wander

import androidx.compose.runtime.Composable

/** Where each tab goes. Home stays on this shell. */
class WanderHomeLinks(
    val weather: @Composable () -> Unit,
    val places: @Composable () -> Unit,
    val onOpenHub: () -> Unit,
)
