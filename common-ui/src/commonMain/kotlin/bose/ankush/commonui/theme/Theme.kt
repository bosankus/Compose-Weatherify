package bose.ankush.commonui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Shared Weatherify Material 3 theme used by both Android and iOS hosts.
 *
 * [isDynamicColor] is honored only on Android 12+ (via [platformDynamicColorScheme]).
 * On iOS (and older Android) it is ignored and the static light/dark schemes below apply.
 * Default is `false` so iOS startup stays free of Android-only APIs; the Android host
 * should pass `isDynamicColor = true` to preserve the previous Android default.
 */
@Composable
fun WeatherifyTheme(
    isDynamicColor: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val staticScheme =
        remember(darkTheme) {
            if (darkTheme) DarkColorPalette else LightColorPalette
        }
    val colors =
        if (isDynamicColor) {
            platformDynamicColorScheme(darkTheme) ?: staticScheme
        } else {
            staticScheme
        }

    PlatformSystemBarsEffect(darkTheme)

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}

/** Android 12+ dynamic Material You scheme, or `null` when unavailable / unsupported. */
@Composable
internal expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** Platform chrome (status/nav bars). No-op on iOS. */
@Composable
internal expect fun PlatformSystemBarsEffect(darkTheme: Boolean)

private val DarkColorPalette = darkColorScheme()

private val LightColorPalette = lightColorScheme()
