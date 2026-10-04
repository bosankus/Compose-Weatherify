package bose.ankush.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformHomeEntry(
    weather: @Composable () -> Unit,
    places: @Composable () -> Unit,
    onOpenHub: () -> Unit,
) {
    weather()
}
