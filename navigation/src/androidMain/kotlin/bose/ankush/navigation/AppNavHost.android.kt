package bose.ankush.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay

@Composable
actual fun AppNavHost(
    entries: List<NavEntry<NavKey>>,
    onBack: () -> Unit,
) {
    NavDisplay(entries = entries, onBack = onBack)
}
