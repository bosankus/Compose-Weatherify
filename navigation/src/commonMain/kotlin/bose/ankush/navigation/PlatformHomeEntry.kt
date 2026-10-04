package bose.ankush.navigation

import androidx.compose.runtime.Composable

/**
 * Android shows the new home shell. iOS keeps the classic home.
 * The lambdas are only composed by the platform that uses them.
 */
@Composable
expect fun PlatformHomeEntry(
    weather: @Composable () -> Unit,
    places: @Composable () -> Unit,
    onOpenHub: () -> Unit,
)
