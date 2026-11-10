package bose.ankush.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Android shows the new home screen, with Settings as its hub page. iOS keeps the classic home.
 * The lambdas are only composed by the platform that uses them.
 *
 * [hub] draws Settings as a page: it leaves `bottomInset` clear for the home tab bar and reports
 * through `onCoverChange` how to close a plan sheet or web page covering it.
 */
@Composable
expect fun PlatformHomeEntry(
    weather: @Composable () -> Unit,
    places: @Composable () -> Unit,
    hub: @Composable (bottomInset: Dp, onCoverChange: (close: (() -> Unit)?) -> Unit) -> Unit,
)
