package bose.ankush.commonui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bose.ankush.commonui.constants.WeatherIconConstants

/**
 * Holds color values for weather icons that adapt to light/dark theme.
 */
class WeatherIconColors(
    val sunColor: Color,
    val sunGlowColor: Color,
    val cloudColor: Color,
    val rainColor: Color,
    val snowColor: Color,
    val thunderColor: Color,
    val fogColor: Color,
) {
    companion object {
        /**
         * Creates theme-aware colors for weather icons.
         */
        @Composable
        fun default(isDarkTheme: Boolean = isSystemInDarkTheme()): WeatherIconColors {
            // Use a try-catch to handle cases where MaterialTheme is not available
            val sunColor =
                try {
                    if (isDarkTheme) Color(0xFFFFD700) else Color(0xFFFF9800)
                } catch (_: Exception) {
                    Color(0xFFFF9800) // Default fallback
                }

            val sunGlowColor =
                try {
                    if (isDarkTheme) {
                        Color(0xFFFFD700).copy(alpha = WeatherIconConstants.SUN_GLOW_ALPHA)
                    } else {
                        Color(0xFFFF9800).copy(alpha = WeatherIconConstants.SUN_GLOW_ALPHA)
                    }
                } catch (_: Exception) {
                    Color(0xFFFF9800).copy(alpha = WeatherIconConstants.SUN_GLOW_ALPHA) // Default fallback
                }

            return WeatherIconColors(
                sunColor = sunColor,
                sunGlowColor = sunGlowColor,
                cloudColor = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.White,
                rainColor = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF2196F3),
                snowColor = if (isDarkTheme) Color.White else Color.White.copy(alpha = 0.9f),
                thunderColor = if (isDarkTheme) Color(0xFFFFEB3B) else Color(0xFFFFC107),
                fogColor =
                    if (isDarkTheme) {
                        Color.LightGray.copy(alpha = 0.7f)
                    } else {
                        Color.Gray.copy(
                            alpha = 0.5f,
                        )
                    },
            )
        }
    }
}

/**
 * A composable that displays an animated weather icon based on the weather description.
 * Maps the description to the appropriate WeatherCondition and renders the corresponding animation.
 * Optimized for performance and supports dark mode.
 *
 * @param weatherDescription The description of the weather condition
 * @param modifier Modifier to be applied to the icon
 * @param colors Theme-aware colors for the weather icons
 */
@Composable
fun AnimatedWeatherIcon(
    weatherDescription: String?,
    modifier: Modifier = Modifier.size(48.dp),
    colors: WeatherIconColors = WeatherIconColors.default(),
) {
    // Map the weather description to a WeatherCondition
    val weatherCondition =
        remember(weatherDescription) {
            mapToWeatherCondition(weatherDescription)
        }

    // Create a content description for accessibility
    val contentDesc =
        remember(weatherCondition) {
            "Weather icon: ${weatherCondition.description}"
        }

    // Determine which animations are needed based on weather condition
    val needsSunAnimation =
        remember(weatherCondition) {
            weatherCondition == WeatherCondition.CLEAR_SKY ||
                weatherCondition == WeatherCondition.FEW_CLOUDS
        }

    val needsCloudAnimation =
        remember(weatherCondition) {
            weatherCondition in
                    listOf(
                        WeatherCondition.FEW_CLOUDS,
                        WeatherCondition.SCATTERED_CLOUDS,
                        WeatherCondition.BROKEN_CLOUDS,
                        WeatherCondition.OVERCAST_CLOUDS,
                    ) || weatherCondition.description.contains("rain") ||
                weatherCondition.description.contains("drizzle") ||
                weatherCondition.description.contains("snow") ||
                weatherCondition.description.contains("thunderstorm")
        }

    val needsRainAnimation =
        remember(weatherCondition) {
            weatherCondition.description.contains("rain") ||
                weatherCondition.description.contains("drizzle") ||
                weatherCondition.description.contains("thunderstorm")
        }

    val needsSnowAnimation =
        remember(weatherCondition) {
            weatherCondition.description.contains("snow") ||
                weatherCondition.description.contains("sleet")
        }

    val needsThunderAnimation =
        remember(weatherCondition) {
            weatherCondition.description.contains("thunderstorm")
        }

    val needsFogAnimation =
        remember(weatherCondition) {
            weatherCondition in
                    listOf(
                        WeatherCondition.MIST,
                        WeatherCondition.SMOKE,
                        WeatherCondition.HAZE,
                        WeatherCondition.SAND_DUST_WHIRLS,
                        WeatherCondition.FOG,
                        WeatherCondition.SAND,
                        WeatherCondition.DUST,
                        WeatherCondition.VOLCANIC_ASH,
                        WeatherCondition.SQUALLS,
                        WeatherCondition.TORNADO,
                    )
        }

    // Animation specs - define once to use as keys in LaunchedEffect
    val sunAnimSpec =
        remember {
            infiniteRepeatable<Float>(
                animation =
                    tween(
                        durationMillis = WeatherIconConstants.SUN_ANIMATION_DURATION,
                        easing = EaseInOutCubic,
                    ),
                repeatMode = RepeatMode.Reverse,
            )
        }

    val cloudAnimSpec =
        remember {
            infiniteRepeatable<Float>(
                animation =
                    tween(
                        durationMillis = WeatherIconConstants.CLOUD_ANIMATION_DURATION,
                        easing = EaseInOutCubic,
                    ),
                repeatMode = RepeatMode.Restart,
            )
        }

    val rainAnimSpec =
        remember {
            infiniteRepeatable<Float>(
                animation =
                    tween(
                        durationMillis = WeatherIconConstants.RAIN_ANIMATION_DURATION,
                        easing = LinearEasing,
                    ),
                repeatMode = RepeatMode.Restart,
            )
        }

    val snowAnimSpec =
        remember {
            infiniteRepeatable<Float>(
                animation =
                    tween(
                        durationMillis = WeatherIconConstants.SNOW_ANIMATION_DURATION,
                        easing = LinearEasing,
                    ),
                repeatMode = RepeatMode.Restart,
            )
        }

    val thunderAnimSpec =
        remember {
            infiniteRepeatable<Float>(
                animation =
                    tween(
                        durationMillis = WeatherIconConstants.THUNDER_ANIMATION_DURATION,
                        easing = FastOutSlowInEasing,
                    ),
                repeatMode = RepeatMode.Restart,
            )
        }

    // Animation states - only initialize what's needed
    val sunGlow = remember { Animatable(0f) }
    val cloudDrift = remember { Animatable(0f) }
    val rainDrop = remember { Animatable(0f) }
    val snowFall = remember { Animatable(0f) }
    val thunderFlash = remember { Animatable(0f) }

    // Start animations only if needed, with proper keys to restart when specs change
    if (needsSunAnimation) {
        LaunchedEffect(weatherCondition, sunAnimSpec) {
            sunGlow.animateTo(
                targetValue = 1f,
                animationSpec = sunAnimSpec,
            )
        }
    }

    if (needsCloudAnimation || needsFogAnimation) {
        LaunchedEffect(weatherCondition, cloudAnimSpec) {
            cloudDrift.animateTo(
                targetValue = 1f,
                animationSpec = cloudAnimSpec,
            )
        }
    }

    if (needsRainAnimation) {
        LaunchedEffect(weatherCondition, rainAnimSpec) {
            rainDrop.animateTo(
                targetValue = 1f,
                animationSpec = rainAnimSpec,
            )
        }
    }

    if (needsSnowAnimation) {
        LaunchedEffect(weatherCondition, snowAnimSpec) {
            snowFall.animateTo(
                targetValue = 1f,
                animationSpec = snowAnimSpec,
            )
        }
    }

    if (needsThunderAnimation) {
        LaunchedEffect(weatherCondition, thunderAnimSpec) {
            thunderFlash.animateTo(
                targetValue = 1f,
                animationSpec = thunderAnimSpec,
            )
        }
    }

    Box(
        modifier =
            modifier.semantics {
                contentDescription = contentDesc
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            when {
                // Clear sky
                weatherCondition == WeatherCondition.CLEAR_SKY -> {
                    drawSun(
                        animationProgress = sunGlow.value,
                        sunColor = colors.sunColor,
                        sunGlowColor = colors.sunGlowColor,
                    )
                }

                // Clouds
                weatherCondition in
                        listOf(
                            WeatherCondition.FEW_CLOUDS,
                            WeatherCondition.SCATTERED_CLOUDS,
                            WeatherCondition.BROKEN_CLOUDS,
                            WeatherCondition.OVERCAST_CLOUDS,
                        )
                    -> {
                    val cloudiness =
                        when (weatherCondition) {
                            WeatherCondition.FEW_CLOUDS -> 0.2f
                            WeatherCondition.SCATTERED_CLOUDS -> 0.4f
                            WeatherCondition.BROKEN_CLOUDS -> 0.7f
                            WeatherCondition.OVERCAST_CLOUDS -> 1.0f
                            else -> 0.5f
                        }

                    if (weatherCondition == WeatherCondition.FEW_CLOUDS) {
                        drawSun(
                            animationProgress = sunGlow.value,
                            scale = 0.7f,
                            offsetX = -size.width * 0.15f,
                            sunColor = colors.sunColor,
                            sunGlowColor = colors.sunGlowColor,
                        )
                    }

                    drawClouds(
                        animationProgress = cloudDrift.value,
                        cloudiness = cloudiness,
                        cloudColor = colors.cloudColor,
                    )
                }

                // Rain
                weatherCondition.description.contains("rain") &&
                        !weatherCondition.description.contains(
                            "thunderstorm",
                        )
                    -> {
                    val intensity =
                        when {
                            weatherCondition.description.contains("light") -> 0.3f
                            weatherCondition.description.contains("heavy") ||
                                weatherCondition.description.contains("intense") ||
                                weatherCondition.description.contains("extreme") -> 0.9f

                            else -> 0.6f
                        }

                    drawClouds(
                        animationProgress = cloudDrift.value,
                        cloudiness = 0.8f,
                        cloudColor = colors.cloudColor,
                    )
                    drawRain(
                        animationProgress = rainDrop.value,
                        intensity = intensity,
                        rainColor = colors.rainColor,
                    )
                }

                // Snow
                weatherCondition.description.contains("snow") ||
                        weatherCondition.description.contains(
                            "sleet",
                        )
                    -> {
                    val intensity =
                        when {
                            weatherCondition.description.contains("light") -> 0.3f
                            weatherCondition.description.contains("heavy") -> 0.9f
                            else -> 0.6f
                        }

                    drawClouds(
                        animationProgress = cloudDrift.value,
                        cloudiness = 0.7f,
                        cloudColor = colors.cloudColor,
                    )
                    drawSnow(
                        animationProgress = snowFall.value,
                        intensity = intensity,
                        snowColor = colors.snowColor,
                    )
                }

                // Thunderstorm
                weatherCondition.description.contains("thunderstorm") -> {
                    drawClouds(
                        animationProgress = cloudDrift.value,
                        cloudiness = 0.9f,
                        cloudColor = colors.cloudColor,
                    )
                    drawRain(
                        animationProgress = rainDrop.value,
                        intensity = 0.7f,
                        rainColor = colors.rainColor,
                    )
                    drawThunder(
                        animationProgress = thunderFlash.value,
                        thunderColor = colors.thunderColor,
                    )
                }

                // Drizzle
                weatherCondition.description.contains("drizzle") -> {
                    drawClouds(
                        animationProgress = cloudDrift.value,
                        cloudiness = 0.7f,
                        cloudColor = colors.cloudColor,
                    )
                    drawRain(
                        animationProgress = rainDrop.value,
                        intensity = 0.3f,
                        rainColor = colors.rainColor,
                    )
                }

                // Atmosphere (mist, fog, etc.)
                weatherCondition in
                        listOf(
                            WeatherCondition.MIST,
                            WeatherCondition.SMOKE,
                            WeatherCondition.HAZE,
                            WeatherCondition.SAND_DUST_WHIRLS,
                            WeatherCondition.FOG,
                            WeatherCondition.SAND,
                            WeatherCondition.DUST,
                            WeatherCondition.VOLCANIC_ASH,
                            WeatherCondition.SQUALLS,
                            WeatherCondition.TORNADO,
                        )
                    -> {
                    drawFog(
                        animationProgress = cloudDrift.value,
                        fogColor = colors.fogColor,
                    )
                }

                // Default fallback
                else -> {
                    drawSun(
                        animationProgress = sunGlow.value,
                        sunColor = colors.sunColor,
                        sunGlowColor = colors.sunGlowColor,
                    )
                }
            }
        }
    }
}
