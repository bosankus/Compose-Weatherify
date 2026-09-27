package bose.ankush.commonui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

@Composable
internal actual fun PlatformSystemBarsEffect(darkTheme: Boolean) {
    // System chrome is owned by the SwiftUI / UIKit host on iOS.
}
