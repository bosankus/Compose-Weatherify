package bose.ankush.weatherify.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Plum-and-crimson dial palette. Every screen sits on true black (AMOLED pixels off), and
 * colour only appears as dial segments and accents, ramping from deep plum to signal red.
 */
internal object DialColors {
    val Background = Color(0xFF000000)

    /** Dial wedges, coolest to hottest. The hottest carries the most "urgent" reading. */
    val Segments =
        listOf(
            Color(0xFF3B1238),
            Color(0xFF5E1446),
            Color(0xFF8E1150),
            Color(0xFFBC0F42),
            Color(0xFFE3182B),
        )

    /** The quiet lower part of the main dial, behind the location/alert line. */
    val DialBase = Color(0xFF2A0D26)
    val Hub = Color(0xFF16090F)

    val Crimson = Color(0xFFE8192C)
    val Magenta = Color(0xFFA3124F)

    /** Unselected round tiles and list rows. */
    val Tile = Color(0xFF1E1C20)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF9C989E)

    val AccentGradient: Brush = Brush.linearGradient(listOf(Magenta, Crimson))
}

private val DialColorScheme =
    ColorScheme(
        primary = DialColors.Crimson,
        primaryDim = DialColors.Magenta,
        primaryContainer = DialColors.Magenta,
        onPrimary = DialColors.TextPrimary,
        onPrimaryContainer = DialColors.TextPrimary,
        secondary = DialColors.Magenta,
        onSecondary = DialColors.TextPrimary,
        surfaceContainerLow = DialColors.Hub,
        surfaceContainer = DialColors.Tile,
        surfaceContainerHigh = Color(0xFF2A272C),
        onSurface = DialColors.TextPrimary,
        onSurfaceVariant = DialColors.TextMuted,
        background = DialColors.Background,
        onBackground = DialColors.TextPrimary,
    )

@Composable
internal fun WeatherifyWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DialColorScheme, content = content)
}
