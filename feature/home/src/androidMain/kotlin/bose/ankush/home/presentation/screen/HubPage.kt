package bose.ankush.home.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Hub page: account and settings, the outer pager's second page. The host supplies the content
 * through [HomeScreenLinks.hub]. It has no fill of its own: the home photo behind the shell pager
 * shows through, so a swipe that arrives before it is built sees the backdrop, not a flash.
 */
@Composable
internal fun HubPage(
    hub: HubPageContent,
    shellPager: PagerState,
    onCoverChange: (close: (() -> Unit)?) -> Unit,
) {
    val built = rememberBuiltAtRest(pager = shellPager, page = HUB_PAGE)
    Box(modifier = Modifier.fillMaxSize()) {
        if (built) hub(TabBarReserveHeight, onCoverChange)
    }
}

/**
 * True once a heavy page behind [pager] may be built. Never mid-swipe: it waits for the pager
 * to rest, either [delayMillis] after the screen appears (so a cold start spends its time on the
 * forecast) or as soon as [page] is reached, whichever comes first.
 */
@Composable
internal fun rememberBuiltAtRest(
    pager: PagerState,
    page: Int,
    delayMillis: Long = BUILD_AT_REST_DELAY_MILLIS,
): Boolean {
    var built by remember { mutableStateOf(false) }
    LaunchedEffect(pager, page) {
        withTimeoutOrNull(delayMillis) {
            snapshotFlow { pager.settledPage }.first { it == page }
        }
        snapshotFlow { pager.isScrollInProgress }.first { !it }
        built = true
    }
    return built
}

/** Draws the hub: (room to leave under the content for the tab bar, cover reporting). */
typealias HubPageContent = @Composable (
    bottomInset: Dp,
    onCoverChange: (close: (() -> Unit)?) -> Unit,
) -> Unit

internal const val SHELL_PAGE_COUNT = 2
internal const val HOME_SHELL_PAGE = 0
internal const val HUB_PAGE = 1
private const val BUILD_AT_REST_DELAY_MILLIS = 3_000L
