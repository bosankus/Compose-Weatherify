package bose.ankush.sunriseui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun SunriseSunsetCombinedAnimation(
    sunriseTimestamp: Int?,
    sunsetTimestamp: Int?,
    currentTimestamp: Long,
    sunIcon: Painter
) {
    // Create a box to contain the animation with enhanced visual effects
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Handle null timestamps
        if (sunriseTimestamp == null || sunsetTimestamp == null) {
            // Enhanced static sun with glow effect
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Sun glow effect
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFB74D).copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = 24.dp.value
                            ),
                            shape = CircleShape
                        )
                )

                Icon(
                    painter = sunIcon,
                    contentDescription = "Sun",
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(32.dp)
                )
            }
            return@Box
        }

        // Convert timestamps to Long for calculations
        val sunrise = sunriseTimestamp.toLong()
        val sunset = sunsetTimestamp.toLong()

        // Calculate the normalized position (0 to 1) based on current time
        val dayDuration = sunset - sunrise
        val timeElapsed = currentTimestamp - sunrise
        val normalizedTimePosition = (timeElapsed.toFloat() / dayDuration).coerceIn(0f, 1f)

        // Determine if it's before sunrise, after sunset, or during the day
        val isBeforeSunrise = currentTimestamp < sunrise
        val isAfterSunset = currentTimestamp > sunset

        // Create animation states with improved easing
        var initialAnimationPlayed by remember { mutableStateOf(false) }
        val animatedProgress = remember { Animatable(0f) }

        // Launch enhanced animation with better easing
        LaunchedEffect(Unit) {
            if (!initialAnimationPlayed) {
                val targetProgress = when {
                    isBeforeSunrise -> 0f
                    isAfterSunset -> 1f
                    else -> normalizedTimePosition
                }

                animatedProgress.animateTo(
                    targetValue = targetProgress,
                    animationSpec = tween(
                        durationMillis = 2000,
                        easing = EaseInOutCubic
                    )
                )
                initialAnimationPlayed = true
            }
        }

        val progress = animatedProgress.value

        // Enhanced sky gradient background with smooth transitions based on time of day
        val skyGradient = createSmoothSkyGradient(progress, isBeforeSunrise, isAfterSunset)

        // Sky background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    brush = skyGradient,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
        )

        // Enhanced arc path calculation with better curve
        val arcWidth = 280f
        val arcHeight = 60f // Reduced from 80f to make arc lower

        // Calculate sun position with improved arc formula
        val xOffset = (progress * arcWidth - arcWidth / 2f).dp

        // Enhanced parabolic curve with more natural arc - adjusted to bring sun closer to arch
        val normalizedX = progress * 2f - 1f
        val yOffset =
            (-arcHeight * (normalizedX * normalizedX * 0.8f) + arcHeight * 0.7f).dp // Reduced multiplier to bring sun lower

        // Draw enhanced arc path with gradient
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Create smoother arc visualization
            for (i in 0..120) {
                val pathProgress = i / 120f
                val pathNormalizedX = pathProgress * 2f - 1f

                // Calculate alpha with smoother fade
                val alpha = when {
                    i < 15 -> (i / 15f) * 0.4f
                    i > 105 -> ((120 - i) / 15f) * 0.4f
                    else -> 0.4f
                }

                // Enhanced height calculation
                val heightPercent = 1 - (pathNormalizedX * pathNormalizedX * 0.8f)
                val pathHeight = (arcHeight * heightPercent).dp

                // Path gradient colors based on time of day and position
                val pathGradient = when {
                    pathProgress < 0.3f -> {
                        // Morning gradient - soft orange to warm yellow
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF7043).copy(alpha = alpha * 0.8f), // Soft orange
                                Color(0xFFFFB74D).copy(alpha = alpha), // Warm yellow
                                Color(0xFFFFC107).copy(alpha = alpha * 0.6f) // Light yellow
                            )
                        )
                    }

                    pathProgress > 0.7f -> {
                        // Evening gradient - golden to deep orange
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF5722).copy(alpha = alpha * 0.8f), // Deep orange
                                Color(0xFFFF7043).copy(alpha = alpha), // Orange
                                Color(0xFFFFB74D).copy(alpha = alpha * 0.6f) // Golden
                            )
                        )
                    }

                    else -> {
                        // Midday gradient - bright yellow to golden
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFC107).copy(alpha = alpha), // Bright yellow
                                Color(0xFFFFB74D).copy(alpha = alpha * 0.8f), // Golden
                                Color(0xFFFF9800).copy(alpha = alpha * 0.6f) // Amber
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(pathHeight)
                        .width(2.dp)
                        .background(
                            brush = pathGradient
                        )
                )
            }
        }

        // Enhanced sun with glow effect and dynamic sizing
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(x = xOffset, y = -yOffset)
        ) {
            // Dynamic sun glow based on position
            val glowSize = (40 + (20 * kotlin.math.sin(progress * kotlin.math.PI))).dp
            val glowAlpha = 0.2f + (0.3f * kotlin.math.sin(progress * kotlin.math.PI).toFloat())

            // Sun glow effect
            Box(
                modifier = Modifier
                    .size(glowSize)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                getSunColor(progress).copy(alpha = glowAlpha),
                                Color.Transparent
                            ),
                            radius = glowSize.value / 2
                        ),
                        shape = CircleShape
                    )
            )

            // Main sun icon with dynamic color and size
            val sunSize = (28 + (8 * kotlin.math.sin(progress * kotlin.math.PI))).dp

            Icon(
                painter = sunIcon,
                contentDescription = "Sun",
                tint = getSunColor(progress),
                modifier = Modifier.size(sunSize)
            )
        }
    }
}

// Helper function to interpolate between two colors
private fun lerpColor(color1: Color, color2: Color, fraction: Float): Color {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = color1.red + (color2.red - color1.red) * clampedFraction,
        green = color1.green + (color2.green - color1.green) * clampedFraction,
        blue = color1.blue + (color2.blue - color1.blue) * clampedFraction,
        alpha = color1.alpha + (color2.alpha - color1.alpha) * clampedFraction
    )
}

// Helper function to create smooth sky gradient with seamless transitions
private fun createSmoothSkyGradient(
    progress: Float,
    isBeforeSunrise: Boolean,
    isAfterSunset: Boolean
): Brush {
    // Define base colors for different times of day
    val dawnColors = listOf(
        Color(0xFF1A237E).copy(alpha = 0.1f), // Deep blue (top - lighter)
        Color(0xFF3F51B5).copy(alpha = 0.3f), // Lighter blue (middle - stronger)
        Color(0xFFFF7043).copy(alpha = 0.4f)  // Orange hint (bottom - most visible)
    )

    val dayColors = listOf(
        Color(0xFF2196F3).copy(alpha = 0.1f), // Sky blue (top - lighter)
        Color(0xFF03A9F4).copy(alpha = 0.2f), // Light blue (middle - stronger)
        Color(0xFFFFEB3B).copy(alpha = 0.3f)  // Yellow hint (bottom - most visible)
    )

    val duskColors = listOf(
        Color(0xFFFF5722).copy(alpha = 0.1f), // Orange (top - lighter)
        Color(0xFFFF9800).copy(alpha = 0.3f), // Amber (middle - stronger)
        Color(0xFF673AB7).copy(alpha = 0.4f)  // Purple (bottom - most visible)
    )

    // Handle edge cases first
    if (isBeforeSunrise) {
        return Brush.verticalGradient(colors = dawnColors)
    }
    if (isAfterSunset) {
        return Brush.verticalGradient(colors = duskColors)
    }

    // Smooth transitions based on progress
    val interpolatedColors = when {
        progress <= 0.2f -> {
            // Smooth transition from dawn to day (0.0 to 0.2)
            val transitionFactor = (progress / 0.2f).coerceIn(0f, 1f)
            listOf(
                lerpColor(dawnColors[0], dayColors[0], transitionFactor),
                lerpColor(dawnColors[1], dayColors[1], transitionFactor),
                lerpColor(dawnColors[2], dayColors[2], transitionFactor)
            )
        }

        progress >= 0.8f -> {
            // Smooth transition from day to dusk (0.8 to 1.0)
            val transitionFactor = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
            listOf(
                lerpColor(dayColors[0], duskColors[0], transitionFactor),
                lerpColor(dayColors[1], duskColors[1], transitionFactor),
                lerpColor(dayColors[2], duskColors[2], transitionFactor)
            )
        }

        else -> {
            // Pure day colors (0.2 to 0.8)
            dayColors
        }
    }

    return Brush.verticalGradient(colors = interpolatedColors)
}

// Helper function to get sun color based on progress
private fun getSunColor(progress: Float): Color {
    return when {
        progress < 0.2f -> {
            // Sunrise colors - soft orange to yellow
            val factor = progress / 0.2f
            Color(0xFFFF7043).copy(
                red = 1f,
                green = 0.44f + (0.36f * factor), // 0.44 to 0.8
                blue = 0.26f + (0.24f * factor)   // 0.26 to 0.5
            )
        }

        progress < 0.8f -> {
            // Midday colors - bright yellow
            Color(0xFFFFC107)
        }

        else -> {
            // Sunset colors - yellow to deep orange
            val factor = (progress - 0.8f) / 0.2f
            Color(0xFFFFC107).copy(
                red = 1f,
                green = 0.76f - (0.32f * factor), // 0.76 to 0.44
                blue = 0.03f + (0.23f * factor)   // 0.03 to 0.26
            )
        }
    }
}