package bose.ankush.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

/**
 * Minimal stand-in for `NavDisplay` until `androidx.navigation3:navigation3-ui` ships an iOS
 * artifact. Renders the top-of-stack entry directly; iOS has no hardware/gesture "system back"
 * the way Android does, so [onBack] here is only invoked by in-content back actions (e.g. a
 * screen's own back button calling into [AppNavigator]), not a platform back handler.
 *
 * Note: Crossfade is deliberately avoided here because its transition keeps both the outgoing
 * and incoming entries composed simultaneously, which triggers "Key … was used multiple times"
 * from SaveableStateHolder when the same NavKey appears in both slots.
 */
@Composable
actual fun AppNavHost(
    entries: List<NavEntry<NavKey>>,
    onBack: () -> Unit,
) {
    val topEntry = entries.lastOrNull() ?: return
    key(topEntry.contentKey) {
        topEntry.Content()
    }
}
