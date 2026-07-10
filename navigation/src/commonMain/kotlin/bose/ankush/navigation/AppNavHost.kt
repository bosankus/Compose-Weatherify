package bose.ankush.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

/**
 * Renders the current top-level tab's back stack.
 *
 * Backed by `androidx.navigation3:navigation3-ui`'s `NavDisplay` on Android (full predictive-back
 * and transition support). That library has no iOS artifact yet, so the iOS actual renders the
 * top-of-stack [NavEntry] directly with a simple crossfade instead — swap this out for `NavDisplay`
 * once it ships for iOS.
 */
@Composable
expect fun AppNavHost(
    entries: List<NavEntry<NavKey>>,
    onBack: () -> Unit,
)
