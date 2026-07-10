package bose.ankush.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

/**
 * Minimal stand-in for `NavDisplay` until `androidx.navigation3:navigation3-ui` ships an iOS
 * artifact. Renders the top-of-stack entry with a crossfade; iOS has no hardware/gesture "system
 * back" the way Android does, so [onBack] here is only invoked by in-content back actions
 * (e.g. a screen's own back button calling into [AppNavigator]), not a platform back handler.
 */
@Composable
actual fun AppNavHost(
    entries: List<NavEntry<NavKey>>,
    onBack: () -> Unit,
) {
    val topEntry = entries.lastOrNull() ?: return
    Crossfade(targetState = topEntry.contentKey) {
        topEntry.Content()
    }
}
