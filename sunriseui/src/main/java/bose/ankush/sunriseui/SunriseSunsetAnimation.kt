/**
 * Dynamic sunrise/sunset landscape animation that responds to real-time data.
 *
 * Features:
 * - Sky gradients that transition between night, dawn, day, and dusk
 * - Animated sun and moon with realistic arc movement
 * - Twinkling stars during night hours
 * - Wind-driven cloud animation during daytime
 * - Atmospheric glow effects around celestial bodies
 *
 * @param sunriseTimestamp Unix timestamp (seconds) for sunrise. Null shows fallback gradient
 * @param sunsetTimestamp Unix timestamp (seconds) for sunset. Null shows fallback gradient
 * @param currentTimestamp Unix timestamp (seconds) for current time
 * @param windDirection Wind direction in degrees (0-360°) for cloud movement. Default 225° (SW)
 */

package bose.ankush.sunriseui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main composable function that creates the animated sunrise/sunset landscape.
 * See class-level documentation above for detailed feature descriptions and usage examples.
 */
@Composable
fun SunriseSunsetCombinedAnimation(
    sunriseTimestamp: Long?,
    sunsetTimestamp: Long?,
    currentTimestamp: Long,
    windDirection: Float = 225f
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SunriseConstants.Dimensions.ANIMATION_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        if (sunriseTimestamp == null || sunsetTimestamp == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SunriseConstants.Dimensions.ANIMATION_HEIGHT)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = SunriseConstants.Colors.DEFAULT_GRADIENT
                        ),
                        shape = RoundedCornerShape(
                            topStart = SunriseConstants.Dimensions.CORNER_RADIUS,
                            topEnd = SunriseConstants.Dimensions.CORNER_RADIUS
                        )
                    )
            )
            return@Box
        }

        val sunrise = sunriseTimestamp
        val sunset = sunsetTimestamp
        // Calculate the normalized position (0 to 1) based on current time
        val dayDuration = sunset - sunrise
        val timeElapsed = currentTimestamp - sunrise
        val normalizedTimePosition = if (dayDuration == 0L) {
            0f // Safe default value if duration is zero
        } else {
            (timeElapsed.toFloat() / dayDuration).coerceIn(0f, 1f)
        }

        val isBeforeSunrise = currentTimestamp < sunrise
        val isAfterSunset = currentTimestamp > sunset
        val isNight = isBeforeSunrise || isAfterSunset

        var initialAnimationPlayed by remember { mutableStateOf(false) }
        val animatedProgress = remember { Animatable(0f) }
        val starTwinkle = remember { Animatable(0f) }
        val atmosphericGlow = remember { Animatable(0f) }
        val cloudDrift = remember { Animatable(0f) }
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
                        durationMillis = 3000,
                        easing = FastOutSlowInEasing
                    )
                )
                initialAnimationPlayed = true
            }
        }

        LaunchedEffect(Unit) {
            starTwinkle.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = SunriseConstants.Durations.STAR_TWINKLE,
                        easing = EaseInOutCubic
                    ),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        LaunchedEffect(Unit) {
            atmosphericGlow.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = SunriseConstants.Durations.ATMOSPHERIC_GLOW,
                        easing = EaseInOutCubic
                    ),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        LaunchedEffect(Unit) {
            cloudDrift.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = SunriseConstants.Durations.CLOUD_DRIFT,
                        easing = EaseInOutCubic
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        }

        val progress = animatedProgress.value
        val currentWindDirection = windDirection
        // Replace multiple Box composables with a single Canvas for arc gradient
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(SunriseConstants.Dimensions.ANIMATION_HEIGHT)
        ) {
            // Make arc invisible by not drawing it
            // (No drawPath or drawArc call here)
        }

        val skyGradient = createSoothingSkyGradient(progress, isBeforeSunrise, isAfterSunset)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SunriseConstants.Dimensions.ANIMATION_HEIGHT)
                .background(
                    brush = if (isNight) Brush.verticalGradient(colors = SunriseConstants.Colors.NIGHT_GRADIENT) else skyGradient,
                    shape = RoundedCornerShape(
                        topStart = SunriseConstants.Dimensions.CORNER_RADIUS,
                        topEnd = SunriseConstants.Dimensions.CORNER_RADIUS
                    )
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(SunriseConstants.Dimensions.ANIMATION_HEIGHT)
        ) {
            val isMorning =
                !isBeforeSunrise && !isAfterSunset && progress <= SunriseConstants.TimeThresholds.MORNING_END
            val isDaytime = !isBeforeSunrise && !isAfterSunset

            if (isNight) {
                drawStarField(
                    progress = progress,
                    twinkleIntensity = starTwinkle.value,
                    isBeforeSunrise = isBeforeSunrise
                )

                drawMoon(
                    progress = progress,
                    isBeforeSunrise = isBeforeSunrise,
                    atmosphericIntensity = atmosphericGlow.value,
                    currentTimestamp = currentTimestamp,
                    sunriseTimestamp = sunriseTimestamp,
                    sunsetTimestamp = sunsetTimestamp
                )
            }

            if (isDaytime) {
                drawSun(
                    progress = progress,
                    atmosphericIntensity = atmosphericGlow.value,
                    currentTimestamp = currentTimestamp,
                    sunriseTimestamp = sunriseTimestamp.toInt(),
                    sunsetTimestamp = sunsetTimestamp.toInt()
                )
            }

            // Draw clouds during daytime with wind-based movement
            if (isDaytime) {
                drawClouds(
                    progress = progress,
                    cloudDriftProgress = cloudDrift.value,
                    windDirection = currentWindDirection,
                    isBeforeSunrise = isBeforeSunrise,
                    isAfterSunset = isAfterSunset
                )
            }
        }
    }
}

/**
 * Interpolates between two colors based on the given fraction.
 * @param color1 Starting color (fraction = 0.0)
 * @param color2 Ending color (fraction = 1.0)
 * @param fraction Interpolation factor, clamped to [0.0, 1.0]
 */
private fun lerpColor(color1: Color, color2: Color, fraction: Float): Color {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = color1.red + (color2.red - color1.red) * clampedFraction,
        green = color1.green + (color2.green - color1.green) * clampedFraction,
        blue = color1.blue + (color2.blue - color1.blue) * clampedFraction,
        alpha = color1.alpha + (color2.alpha - color1.alpha) * clampedFraction
    )
}

/**
 * Creates sky gradient that transitions between night, dawn, day, and dusk colors.
 * @param progress Normalized time progress (0.0 = sunrise, 1.0 = sunset)
 * @param isBeforeSunrise True if before sunrise
 * @param isAfterSunset True if after sunset
 */
private fun createSoothingSkyGradient(
    progress: Float,
    isBeforeSunrise: Boolean,
    isAfterSunset: Boolean
): Brush {
    val nightColors = SunriseConstants.Colors.NIGHT_GRADIENT

    val dawnColors = SunriseConstants.Colors.DAWN_GRADIENT
    val dayColors = SunriseConstants.Colors.DAY_GRADIENT
    val duskColors = SunriseConstants.Colors.DUSK_GRADIENT

    if (isBeforeSunrise) {
        return Brush.verticalGradient(colors = nightColors)
    }
    if (isAfterSunset) {
        return Brush.verticalGradient(colors = nightColors)
    }
    val interpolatedColors = when {
        progress <= SunriseConstants.TimeThresholds.DAWN_END -> {
            val transitionFactor =
                (progress / SunriseConstants.TimeThresholds.DAWN_END).coerceIn(0f, 1f)
            listOf(
                lerpColor(dawnColors[0], dayColors[0], transitionFactor),
                lerpColor(dawnColors[1], dayColors[1], transitionFactor),
                lerpColor(dawnColors[2], dayColors[2], transitionFactor),
                lerpColor(dawnColors[3], dayColors[3], transitionFactor)
            )
        }

        progress >= SunriseConstants.TimeThresholds.DUSK_START -> {
            val transitionFactor =
                ((progress - SunriseConstants.TimeThresholds.DUSK_START) / SunriseConstants.TimeThresholds.DAWN_END).coerceIn(
                    0f,
                    1f
                )
            listOf(
                lerpColor(dayColors[0], duskColors[0], transitionFactor),
                lerpColor(dayColors[1], duskColors[1], transitionFactor),
                lerpColor(dayColors[2], duskColors[2], transitionFactor),
                lerpColor(dayColors[3], duskColors[3], transitionFactor)
            )
        }

        else -> dayColors
    }

    return Brush.verticalGradient(colors = interpolatedColors)
}


/**
 * Renders twinkling stars across the night sky with varying opacity and size.
 * @param progress Normalized time progress (unused)
 * @param twinkleIntensity Animation value (0.0-1.0) controlling twinkle effect
 * @param isBeforeSunrise True if before sunrise, affects star opacity
 */
private fun DrawScope.drawStarField(
    progress: Float,
    twinkleIntensity: Float,
    isBeforeSunrise: Boolean
) {
    val starCount = SunriseConstants.Counts.STAR_COUNT
    val baseOpacity =
        if (isBeforeSunrise) SunriseConstants.Opacity.STAR_BASE_BEFORE_SUNRISE else SunriseConstants.Opacity.STAR_BASE_AFTER_SUNSET
    val starPositions = SunriseConstants.STAR_POSITIONS

    starPositions.forEachIndexed { index, (xRatio, yRatio) ->
        val x = size.width * xRatio
        val y = size.height * yRatio

        // Create twinkling effect
        val twinkle =
            sin((twinkleIntensity * 2 * PI + index * 0.5).toFloat()) * SunriseConstants.Opacity.TWINKLE_VARIATION + SunriseConstants.Opacity.TWINKLE_BASE
        val starOpacity = baseOpacity * twinkle

        val starSize =
            SunriseConstants.Dimensions.STAR_BASE_SIZE + (SunriseConstants.Dimensions.STAR_SIZE_VARIATION * twinkle)

        drawCircle(
            color = SunriseConstants.Colors.STAR_COLOR.copy(alpha = starOpacity),
            radius = starSize,
            center = androidx.compose.ui.geometry.Offset(x, y)
        )
    }
}

/**
 * Renders animated moon that travels across the night sky in an arc pattern.
 * @param progress Normalized time progress (unused)
 * @param isBeforeSunrise True if before sunrise, affects moon trajectory
 * @param atmosphericIntensity Animation value (0.0-1.0) for glow effect
 * @param currentTimestamp Current Unix timestamp in seconds
 * @param sunriseTimestamp Sunrise timestamp, null for fallback positioning
 * @param sunsetTimestamp Sunset timestamp, null for fallback positioning
 */
private fun DrawScope.drawMoon(
    progress: Float,
    isBeforeSunrise: Boolean,
    atmosphericIntensity: Float,
    currentTimestamp: Long,
    sunriseTimestamp: Long?,
    sunsetTimestamp: Long?
) {
    val moonX: Float
    val moonY: Float

    if (sunriseTimestamp != null && sunsetTimestamp != null) {
        val sunrise = sunriseTimestamp
        val sunset = sunsetTimestamp

        if (isBeforeSunrise) {
            val nightDuration = sunrise - (sunset - 24 * 3600)
            val timeElapsed = currentTimestamp - (sunset - 24 * 3600)
            val nightProgress = (timeElapsed.toFloat() / nightDuration).coerceIn(0f, 1f)
            moonX =
                size.width * (SunriseConstants.Positioning.MOON_START_X - nightProgress * SunriseConstants.Positioning.MOON_TRAVEL_DISTANCE)
            moonY =
                size.height * (SunriseConstants.Positioning.MOON_Y_VARIATION - (sin(nightProgress * PI).toFloat() * SunriseConstants.Positioning.MOON_Y_AMPLITUDE))
        } else {
            val nextSunrise = sunrise + 24 * 3600
            val nightDuration = nextSunrise - sunset
            val timeElapsed = currentTimestamp - sunset
            val nightProgress = (timeElapsed.toFloat() / nightDuration).coerceIn(0f, 1f)
            moonX =
                size.width * (SunriseConstants.Positioning.MOON_END_X + nightProgress * SunriseConstants.Positioning.MOON_TRAVEL_DISTANCE)
            moonY =
                size.height * (SunriseConstants.Positioning.MOON_Y_VARIATION - (sin(nightProgress * PI).toFloat() * SunriseConstants.Positioning.MOON_Y_AMPLITUDE))
        }
    } else {
        moonX = if (isBeforeSunrise) {
            size.width * SunriseConstants.Positioning.MOON_START_X
        } else {
            size.width * SunriseConstants.Positioning.MOON_END_X
        }
        moonY = size.height * SunriseConstants.Positioning.MOON_BASE_Y
    }

    val moonRadius =
        SunriseConstants.Dimensions.MOON_BASE_RADIUS + (SunriseConstants.Dimensions.MOON_RADIUS_VARIATION * atmosphericIntensity)
    val moonOpacity =
        SunriseConstants.Opacity.MOON_BASE + (SunriseConstants.Opacity.MOON_VARIATION * atmosphericIntensity)

    drawCircle(
        color = SunriseConstants.Colors.MOON_COLOR.copy(alpha = moonOpacity * 0.3f),
        radius = moonRadius * 1.5f,
        center = androidx.compose.ui.geometry.Offset(moonX, moonY)
    )

    drawCircle(
        color = SunriseConstants.Colors.MOON_COLOR.copy(alpha = moonOpacity),
        radius = moonRadius,
        center = androidx.compose.ui.geometry.Offset(moonX, moonY)
    )

    val phaseOffset = moonRadius * 0.3f
    drawCircle(
        color = SunriseConstants.Colors.MOON_PHASE_COLOR.copy(alpha = 0.2f),
        radius = moonRadius * 0.8f,
        center = androidx.compose.ui.geometry.Offset(moonX + phaseOffset, moonY)
    )
}

/**
 * Renders animated sun that travels across the sky with dynamic colors and rays.
 * @param progress Normalized time progress (unused)
 * @param atmosphericIntensity Animation value (0.0-1.0) for glow effect
 * @param currentTimestamp Current Unix timestamp in seconds
 * @param sunriseTimestamp Sunrise timestamp (as Int)
 * @param sunsetTimestamp Sunset timestamp (as Int)
 */
private fun DrawScope.drawSun(
    progress: Float,
    atmosphericIntensity: Float,
    currentTimestamp: Long,
    sunriseTimestamp: Int,
    sunsetTimestamp: Int
) {
    val sunrise = sunriseTimestamp.toLong()
    val sunset = sunsetTimestamp.toLong()
    val dayDuration = sunset - sunrise
    val timeElapsed = currentTimestamp - sunrise
    val timeProgress = (timeElapsed.toFloat() / dayDuration).coerceIn(0f, 1f)

    val sunX =
        size.width * (SunriseConstants.Positioning.SUN_START_X + timeProgress * SunriseConstants.Positioning.SUN_TRAVEL_DISTANCE)
    val sunY =
        size.height * (SunriseConstants.Positioning.SUN_BASE_Y - (sin(timeProgress * PI).toFloat() * SunriseConstants.Positioning.SUN_Y_AMPLITUDE))

    val sunRadius =
        SunriseConstants.Dimensions.SUN_BASE_RADIUS + (SunriseConstants.Dimensions.SUN_RADIUS_VARIATION * atmosphericIntensity)
    val sunOpacity =
        SunriseConstants.Opacity.SUN_BASE + (SunriseConstants.Opacity.SUN_VARIATION * atmosphericIntensity)
    val sunColor = when {
        timeProgress < SunriseConstants.TimeThresholds.SUN_MORNING_END -> SunriseConstants.Colors.SUN_EARLY_MORNING
        timeProgress < SunriseConstants.TimeThresholds.SUN_MIDMORNING_END -> SunriseConstants.Colors.SUN_MORNING
        timeProgress < SunriseConstants.TimeThresholds.SUN_EVENING_START -> SunriseConstants.Colors.SUN_MIDDAY
        timeProgress < SunriseConstants.TimeThresholds.SUN_LATE_EVENING_START -> SunriseConstants.Colors.SUN_EVENING
        else -> SunriseConstants.Colors.SUN_LATE_EVENING
    }

    drawCircle(
        color = sunColor.copy(alpha = sunOpacity * 0.3f),
        radius = sunRadius * 1.8f,
        center = androidx.compose.ui.geometry.Offset(sunX, sunY)
    )

    drawCircle(
        color = sunColor.copy(alpha = sunOpacity),
        radius = sunRadius,
        center = androidx.compose.ui.geometry.Offset(sunX, sunY)
    )

    val rayCount = SunriseConstants.Counts.SUN_RAY_COUNT
    val rayLength = SunriseConstants.Dimensions.SUN_RAY_LENGTH
    val rayWidth = SunriseConstants.Dimensions.SUN_RAY_WIDTH

    for (i in 0 until rayCount) {
        val angle = (i * 360f / rayCount + progress * 20f) * PI / 180f
        val startRadius = sunRadius + 5f
        val endRadius = startRadius + rayLength

        val startX = sunX + cos(angle).toFloat() * startRadius
        val startY = sunY + sin(angle).toFloat() * startRadius
        val endX = sunX + cos(angle).toFloat() * endRadius
        val endY = sunY + sin(angle).toFloat() * endRadius

        drawLine(
            color = sunColor.copy(alpha = sunOpacity * 0.6f),
            start = androidx.compose.ui.geometry.Offset(startX, startY),
            end = androidx.compose.ui.geometry.Offset(endX, endY),
            strokeWidth = rayWidth
        )
    }
}

/**
 * Renders animated clouds that drift across the sky based on wind direction.
 * @param progress Normalized time progress for color determination
 * @param cloudDriftProgress Animation value (0.0-1.0) controlling cloud movement
 * @param windDirection Wind direction in degrees (0-360°) affecting movement
 * @param isBeforeSunrise True if before sunrise (unused)
 * @param isAfterSunset True if after sunset (unused)
 */
private fun DrawScope.drawClouds(
    progress: Float,
    cloudDriftProgress: Float,
    windDirection: Float,
    isBeforeSunrise: Boolean,
    isAfterSunset: Boolean
) {
    val cloudCount = SunriseConstants.Counts.CLOUD_COUNT
    val cloudColor = when {
        progress <= SunriseConstants.TimeThresholds.DAWN_END -> SunriseConstants.Colors.CLOUD_DAWN_COLOR
        progress >= SunriseConstants.TimeThresholds.DUSK_START -> SunriseConstants.Colors.CLOUD_DUSK_COLOR
        else -> SunriseConstants.Colors.CLOUD_DAY_COLOR
    }

    val baseOpacity =
        SunriseConstants.Opacity.CLOUD_BASE + (SunriseConstants.Opacity.CLOUD_VARIATION * sin(
            cloudDriftProgress * PI
        ).toFloat())

    // Calculate wind influence on cloud movement
    val windInfluenceX =
        cos(windDirection * PI / 180f).toFloat() * SunriseConstants.Positioning.CLOUD_DRIFT_SPEED
    val windInfluenceY =
        sin(windDirection * PI / 180f).toFloat() * SunriseConstants.Positioning.CLOUD_DRIFT_SPEED * 0.3f

    for (i in 0 until cloudCount) {
        val baseX =
            (i * SunriseConstants.Positioning.CLOUD_SPACING_X + cloudDriftProgress * windInfluenceX) % 1.2f - 0.1f
        val baseY =
            SunriseConstants.Positioning.CLOUD_BASE_Y + (i % 2) * SunriseConstants.Positioning.CLOUD_Y_VARIATION + windInfluenceY

        val cloudX = size.width * baseX
        val cloudY = size.height * baseY

        // Draw cloud as multiple overlapping circles (puffs)
        val puffCount = SunriseConstants.Counts.CLOUD_PUFFS_PER_CLOUD
        val puffRadius = SunriseConstants.Dimensions.CLOUD_PUFF_RADIUS
        val cloudWidth = SunriseConstants.Dimensions.CLOUD_WIDTH

        for (j in 0 until puffCount) {
            val puffX = cloudX + (j - 1) * (cloudWidth / puffCount)
            val puffY = cloudY + sin((j + cloudDriftProgress * 2) * PI).toFloat() * 3f
            val puffSize = puffRadius + (j % 2) * 2f

            drawCircle(
                color = cloudColor.copy(alpha = baseOpacity * 0.8f),
                radius = puffSize,
                center = androidx.compose.ui.geometry.Offset(puffX, puffY)
            )
        }
    }
}
