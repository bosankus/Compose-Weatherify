package bose.ankush.settings.presentation

import androidx.compose.ui.unit.Dp

/** How the host shows Settings. */
sealed interface SettingsPresentation {
    /** Its own destination: a top bar with back, the host's bottom bar, light status bar icons. */
    data object Screen : SettingsPresentation

    /**
     * One page of another screen's pager (the Android home). No top bar or back button, and the
     * host's own tab bar floats over the bottom, so the list leaves [bottomInset] clear.
     *
     * While the plan sheet or a web page covers the page, [onCoverChange] gets the way to close
     * it, and null once it closes. The host hides its tab bar, stops paging, and sends back
     * presses there meanwhile.
     */
    data class Page(
        val bottomInset: Dp,
        val onCoverChange: (close: (() -> Unit)?) -> Unit,
    ) : SettingsPresentation
}
