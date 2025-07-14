package bose.ankush.weatherify.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun WeatherifyTheme(
    isDynamicColor: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Cache dynamic color check to avoid recalculating it
    val dynamicColor = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    // Cache the color scheme calculation to avoid recalculating it on each recomposition
    // Only recalculate when darkTheme or dynamicColor changes
    val colors = remember(darkTheme, dynamicColor) {
        when {
            darkTheme && dynamicColor -> dynamicDarkColorScheme(context)
            darkTheme -> darkColorPalette
            dynamicColor -> dynamicLightColorScheme(context)
            else -> lightColorPalette
        }
    }

    // Cache the system UI controller to avoid recreating it
    val systemUiController = rememberSystemUiController()

    // Only update system UI colors when colors or darkTheme changes
    SideEffect {
        with(systemUiController) {
            // Set both status bar and navigation bar in a single batch update
            setSystemBarsColor(
                color = Transparent,
                darkIcons = !darkTheme
            )
            isNavigationBarVisible = false
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

private val darkColorPalette = darkColorScheme(

)

private val lightColorPalette = lightColorScheme(

)
