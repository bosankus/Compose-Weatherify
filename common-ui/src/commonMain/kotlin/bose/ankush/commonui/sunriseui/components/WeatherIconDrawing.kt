package bose.ankush.commonui.sunriseui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import bose.ankush.commonui.sunriseui.constants.WeatherIconConstants
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a sun with animated glow and rays for the weather icon.
 * Uses theme-aware colors that adapt to light/dark mode.
 */
fun DrawScope.drawSun(
    animationProgress: Float,
    scale: Float = 1.0f,
    offsetX: Float = 0f,
    sunColor: Color,
    sunGlowColor: Color
) {
    val centerX = size.width / 2 + offsetX
    val centerY = size.height / 2
    val radius = size.width.coerceAtMost(size.height) * 0.25f * scale

    // Enhanced glow effect with smoother pulsing
    val glowRadius =
        radius * (1.0f + WeatherIconConstants.SUN_PULSE_SCALE * sin(animationProgress * PI).toFloat())
    drawCircle(
        color = sunGlowColor,
        radius = glowRadius * WeatherIconConstants.SUN_GLOW_SCALE,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )

    // Sun body with slight variation for more natural appearance
    drawCircle(
        color = sunColor,
        radius = radius * (1.0f + WeatherIconConstants.SUN_BODY_VARIATION * sin(animationProgress * PI * 2).toFloat()),
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )

    // More dynamic sun rays with varying lengths
    val rayCount = 8

    for (i in 0 until rayCount) {
        // Vary ray length based on position and animation
        val rayFactor = 0.8f + 0.2f * sin((animationProgress * PI * 2 + i).toFloat())
        val rayLength = radius * 0.6f * rayFactor

        val angle = (i * 360f / rayCount + animationProgress * 20f) * PI / 180f
        val startRadius = radius + 2f
        val endRadius = startRadius + rayLength

        val startX = centerX + cos(angle).toFloat() * startRadius
        val startY = centerY + sin(angle).toFloat() * startRadius
        val endX = centerX + cos(angle).toFloat() * endRadius
        val endY = centerY + sin(angle).toFloat() * endRadius

        // Vary ray thickness for more natural appearance
        val strokeWidth = 2f + 1f * sin((animationProgress * PI * 2 + i * 0.5f).toFloat())

        drawLine(
            color = sunColor.copy(alpha = 0.7f),
            start = androidx.compose.ui.geometry.Offset(startX, startY),
            end = androidx.compose.ui.geometry.Offset(endX, endY),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Draws clouds with smoother animation.
 * Uses theme-aware colors that adapt to light/dark mode.
 */
fun DrawScope.drawClouds(
    animationProgress: Float,
    cloudiness: Float,
    cloudColor: Color
) {
    val cloudCount = (2 + (cloudiness * 2).toInt()).coerceAtMost(4)

    for (i in 0 until cloudCount) {
        // Smoother cloud movement with varying speeds
        val speedFactor = 0.8f + (i % 3) * 0.1f
        val baseX =
            size.width * (0.3f + (i * 0.15f) + animationProgress * WeatherIconConstants.CLOUD_MOVEMENT_SCALE * speedFactor) % size.width
        val baseY =
            size.height * (0.4f + (i % 2) * 0.1f + sin(animationProgress * PI * speedFactor) * 0.02f)

        // Draw cloud as multiple overlapping circles with varying sizes
        val puffCount = 3
        val puffRadius = size.width * 0.1f

        for (j in 0 until puffCount) {
            val puffX = baseX + (j - 1) * (puffRadius * 1.2f)
            val puffY = baseY + sin((j + animationProgress * 1.5f) * PI).toFloat() * 2f
            val puffSize =
                puffRadius * (0.8f + (j % 2) * 0.4f + sin(animationProgress * PI + j) * 0.05f)

            // Vary opacity slightly for more natural appearance
            val alpha =
                WeatherIconConstants.CLOUD_BASE_ALPHA + 0.2f * sin((animationProgress * PI + j * 0.5f).toFloat())

            drawCircle(
                color = cloudColor.copy(alpha = alpha),
                radius = puffSize.toFloat(),
                center = androidx.compose.ui.geometry.Offset(puffX, puffY.toFloat())
            )
        }
    }
}

/**
 * Draws rain with a natural, continuous animation.
 * Features dynamic intensity, varied raindrop appearance, wind effects, and splash effects.
 * Uses theme-aware colors that adapt to light/dark mode.
 */
fun DrawScope.drawRain(
    animationProgress: Float,
    intensity: Float,
    rainColor: Color
) {
    // Increase drop count for heavier rain, with a higher maximum
    val baseDropCount = 8 + (intensity * 25).toInt()
    val dropCount = baseDropCount.coerceAtMost(30)

    // Wind effect - varies over time for natural feel
    val windStrength = sin(animationProgress * PI * 0.3f) * 0.2f + 0.1f

    // Create a pseudo-random distribution of raindrops
    for (i in 0 until dropCount) {
        // Create unique seed for each raindrop to avoid visible patterns
        val seed = (i * 13 + 7) % dropCount

        // Vary drop speeds significantly for more realistic rain
        // Heavier rain falls faster on average
        val speedFactor = 0.6f + (seed % 7) * 0.08f + intensity * 0.3f

        // Create a unique phase for each raindrop to avoid synchronized movement
        val phase = (seed * 0.1f) % 1.0f

        // Non-repeating progress calculation with unique offsets
        // This creates the illusion of continuous rainfall without visible loops
        val uniqueProgress = (animationProgress * speedFactor + phase) % 1.0f

        // Horizontal position with wind effect and slight randomization
        // Wind effect is stronger for lighter drops (smaller thickness)
        val horizontalSeed = (seed * 17 + 3) % dropCount
        val dropThickness = 1.0f + intensity * 1.2f * (0.7f + (seed % 5) * 0.1f)
        val windEffect = windStrength * (1.5f - dropThickness * 0.3f)

        // Initial horizontal position is distributed across the width
        val initialX = size.width * ((horizontalSeed * 0.1f) % 1.0f)

        // Apply wind and slight randomization to horizontal position
        val dropX = initialX + sin(animationProgress * PI * 0.2f + seed) * size.width * 0.05f

        // Vertical position with continuous movement
        val dropY = size.height * (0.3f + uniqueProgress * 0.7f)

        // Vary drop length based on intensity, speed, and randomization
        // Faster drops appear longer (motion blur effect)
        val lengthVariation = 0.7f + (seed % 5) * 0.1f + speedFactor * 0.3f
        val dropLength = size.height * (0.05f + 0.08f * intensity) * lengthVariation

        // Calculate end position with wind slant
        val endX = dropX + windEffect * dropLength
        val endY = dropY + dropLength

        // Vary opacity based on thickness and random factors
        // Thinner drops are more transparent
        val baseAlpha =
            (WeatherIconConstants.RAIN_BASE_ALPHA - 0.2f + 0.4f * (dropThickness / 3.0f))
                .coerceIn(0.3f, 0.9f)
        val alphaVariation = 0.15f * sin((animationProgress * PI * 0.7f + seed).toFloat())
        val dropAlpha = (baseAlpha + alphaVariation).coerceIn(0.2f, 0.95f)

        // Draw the raindrop with slant from wind
        drawLine(
            color = rainColor.copy(alpha = dropAlpha),
            start = androidx.compose.ui.geometry.Offset(dropX.toFloat(), dropY),
            end = androidx.compose.ui.geometry.Offset(endX.toFloat(), endY),
            strokeWidth = dropThickness
        )

        // Add splash effect when drops hit the bottom
        // Only some drops create visible splashes
        if (endY >= size.height * 0.95f && seed % 3 == 0) {
            val splashProgress = (uniqueProgress * 3f) % 1.0f

            // Only show splash at the beginning of its animation cycle
            if (splashProgress < 0.3f) {
                val splashSize = size.width * 0.02f * (1f - splashProgress / 0.3f) * intensity
                val splashAlpha = (0.7f - splashProgress / 0.3f * 0.7f) * intensity * 0.8f

                // Draw splash as a small circle
                drawCircle(
                    color = rainColor.copy(alpha = splashAlpha),
                    radius = splashSize,
                    center = androidx.compose.ui.geometry.Offset(
                        endX.toFloat(),
                        size.height * 0.98f
                    )
                )

                // For heavier rain, add a second splash ripple
                if (intensity > 0.6f && seed % 6 == 0) {
                    val rippleProgress = splashProgress * 1.5f
                    if (rippleProgress < 0.3f) {
                        val rippleSize = splashSize * 2f * (rippleProgress / 0.3f)
                        val rippleAlpha = (0.3f - rippleProgress / 0.3f * 0.3f) * intensity * 0.5f

                        drawCircle(
                            color = rainColor.copy(alpha = rippleAlpha),
                            radius = rippleSize,
                            center = androidx.compose.ui.geometry.Offset(
                                endX.toFloat(),
                                size.height * 0.98f
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws snow with more realistic animation.
 * Uses theme-aware colors that adapt to light/dark mode.
 */
fun DrawScope.drawSnow(
    animationProgress: Float,
    intensity: Float,
    snowColor: Color
) {
    val flakeCount = (5 + (intensity * 15).toInt()).coerceAtMost(20)

    for (i in 0 until flakeCount) {
        // Vary flake speeds and paths for more realistic snow
        val speedFactor = 0.6f + (i % 5) * 0.1f
        val horizontalMovement =
            sin((animationProgress + i * 0.1f) * PI * 2) * size.width * WeatherIconConstants.SNOW_HORIZONTAL_MOVEMENT
        val flakeX = size.width * ((i * 0.1f) % 1.0f) + horizontalMovement
        val flakeProgress = (animationProgress * speedFactor + (i * 0.1f)) % 1.0f
        val flakeY = size.height * (0.5f + flakeProgress * 0.5f)

        // Vary flake size for more natural appearance
        val flakeSize = size.width * (0.015f + 0.01f * (i % 3) / 3f)

        // Draw snowflake (simple circle for now, could be enhanced to actual snowflake shape)
        drawCircle(
            color = snowColor.copy(alpha = WeatherIconConstants.SNOW_BASE_ALPHA + 0.2f * sin((animationProgress * PI + i).toFloat())),
            radius = flakeSize,
            center = androidx.compose.ui.geometry.Offset(flakeX.toFloat(), flakeY)
        )
    }
}

/**
 * Draws thunder with more realistic animation.
 * Uses theme-aware colors that adapt to light/dark mode.
 */
fun DrawScope.drawThunder(
    animationProgress: Float,
    thunderColor: Color
) {
    // Make thunder appear more gradually instead of abruptly
    val flashIntensity = sin(animationProgress * PI * 2).toFloat().coerceIn(0f, 1f)

    if (flashIntensity > 0.2f) {
        val centerX = size.width * 0.5f
        val startY = size.height * 0.4f

        // Draw lightning bolt with varying intensity
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX - size.width * 0.1f, startY + size.height * 0.15f)
            lineTo(centerX, startY + size.height * 0.2f)
            lineTo(centerX - size.width * 0.05f, startY + size.height * 0.4f)
            lineTo(centerX + size.width * 0.1f, startY + size.height * 0.15f)
            lineTo(centerX, startY + size.height * 0.1f)
            close()
        }

        drawPath(
            path = path,
            color = thunderColor.copy(alpha = flashIntensity * WeatherIconConstants.THUNDER_FLASH_ALPHA)
        )

        // Add a glow effect around the lightning
        drawCircle(
            color = thunderColor.copy(alpha = flashIntensity * 0.3f),
            radius = size.width * 0.2f,
            center = androidx.compose.ui.geometry.Offset(centerX, startY + size.height * 0.2f)
        )
    }
}

/**
 * Draws fog with more realistic animation.
 * Uses theme-aware colors that adapt to light/dark mode.
 */
fun DrawScope.drawFog(
    animationProgress: Float,
    fogColor: Color
) {
    val layerCount = 6

    for (i in 0 until layerCount) {
        // Vary layer positions and speeds for more natural fog
        val layerY = size.height * (0.3f + i * 0.1f)
        val layerWidth = size.width * (0.6f + (i % 3) * 0.1f)
        val speedFactor = 0.8f + (i % 3) * 0.1f
        val layerOffset =
            size.width * 0.15f + sin((animationProgress * speedFactor + i * 0.2f) * PI).toFloat() * size.width * 0.08f

        // Vary opacity for more natural appearance
        val alpha =
            WeatherIconConstants.FOG_BASE_ALPHA + 0.2f * sin((animationProgress * PI + i * 0.5f)).toFloat()

        // Draw fog layer with rounded ends for more natural appearance
        drawLine(
            color = fogColor.copy(alpha = alpha),
            start = androidx.compose.ui.geometry.Offset(layerOffset, layerY),
            end = androidx.compose.ui.geometry.Offset(layerOffset + layerWidth, layerY),
            strokeWidth = size.height * (0.02f + 0.01f * (i % 3) / 3f)
        )
    }
}

/**
 * Maps a weather description string to a WeatherCondition enum value.
 * Uses fuzzy matching to handle variations in description text.
 * Improved to handle more real-world API responses.
 */
fun mapToWeatherCondition(description: String?): WeatherCondition {
    if (description.isNullOrBlank()) return WeatherCondition.CLEAR_SKY

    // Convert to lowercase for case-insensitive matching
    val lowerDesc = description.lowercase()

    // Try to find an exact match first
    WeatherCondition.entries.forEach { condition ->
        if (lowerDesc == condition.description.lowercase()) {
            return condition
        }
    }

    // If no exact match, try fuzzy matching based on keywords
    return when {
        "thunderstorm" in lowerDesc -> {
            when {
                "light" in lowerDesc -> WeatherCondition.LIGHT_THUNDERSTORM
                "heavy" in lowerDesc || "strong" in lowerDesc || "severe" in lowerDesc -> WeatherCondition.HEAVY_THUNDERSTORM
                else -> WeatherCondition.THUNDERSTORM
            }
        }

        "drizzle" in lowerDesc -> {
            when {
                "light" in lowerDesc || "slight" in lowerDesc -> WeatherCondition.LIGHT_INTENSITY_DRIZZLE
                "heavy" in lowerDesc || "intense" in lowerDesc -> WeatherCondition.HEAVY_INTENSITY_DRIZZLE
                else -> WeatherCondition.DRIZZLE
            }
        }

        "rain" in lowerDesc -> {
            when {
                "light" in lowerDesc || "slight" in lowerDesc -> WeatherCondition.LIGHT_RAIN
                "heavy" in lowerDesc || "intense" in lowerDesc || "strong" in lowerDesc || "pouring" in lowerDesc -> WeatherCondition.HEAVY_INTENSITY_RAIN
                "shower" in lowerDesc -> WeatherCondition.SHOWER_RAIN
                else -> WeatherCondition.MODERATE_RAIN
            }
        }

        "snow" in lowerDesc -> {
            when {
                "light" in lowerDesc || "slight" in lowerDesc || "flurries" in lowerDesc -> WeatherCondition.LIGHT_SNOW
                "heavy" in lowerDesc || "intense" in lowerDesc || "blizzard" in lowerDesc -> WeatherCondition.HEAVY_SNOW
                else -> WeatherCondition.SNOW
            }
        }

        "sleet" in lowerDesc -> WeatherCondition.SLEET
        "clear" in lowerDesc || "sunny" in lowerDesc || "fair" in lowerDesc -> WeatherCondition.CLEAR_SKY
        "cloud" in lowerDesc -> {
            when {
                "few" in lowerDesc || "11-25%" in lowerDesc || "partly" in lowerDesc -> WeatherCondition.FEW_CLOUDS
                "scattered" in lowerDesc || "25-50%" in lowerDesc -> WeatherCondition.SCATTERED_CLOUDS
                "broken" in lowerDesc || "51-84%" in lowerDesc || "mostly" in lowerDesc -> WeatherCondition.BROKEN_CLOUDS
                "overcast" in lowerDesc || "85-100%" in lowerDesc || "full" in lowerDesc -> WeatherCondition.OVERCAST_CLOUDS
                else -> WeatherCondition.SCATTERED_CLOUDS
            }
        }

        "mist" in lowerDesc -> WeatherCondition.MIST
        "fog" in lowerDesc -> WeatherCondition.FOG
        "haze" in lowerDesc -> WeatherCondition.HAZE
        "smoke" in lowerDesc -> WeatherCondition.SMOKE
        "dust" in lowerDesc || "sand" in lowerDesc -> WeatherCondition.SAND_DUST_WHIRLS
        "tornado" in lowerDesc || "cyclone" in lowerDesc || "hurricane" in lowerDesc -> WeatherCondition.TORNADO
        else -> WeatherCondition.CLEAR_SKY // Default fallback
    }
}
