package bose.ankush.navigation

import androidx.compose.runtime.Composable
import bose.ankush.home.presentation.shell.ShellHomeRoute

@Composable
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
actual fun PlatformHomeEntry(
    weather: @Composable () -> Unit,
    places: @Composable () -> Unit,
    onOpenHub: () -> Unit,
) {
    ShellHomeRoute(
        places = places,
        onOpenSettings = onOpenHub,
    )
}
