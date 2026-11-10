package bose.ankush.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

@Composable
actual fun PlatformHomeEntry(
    weather: @Composable () -> Unit,
    places: @Composable () -> Unit,
    hub: @Composable (bottomInset: Dp, onCoverChange: (close: (() -> Unit)?) -> Unit) -> Unit,
) {
    weather()
}
