package bose.ankush.weatherify.presentation.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun WeatherifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = SFCompactDisplayTypography,
        content = content
    )
}