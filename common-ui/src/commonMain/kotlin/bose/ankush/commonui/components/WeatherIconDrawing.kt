@file:Suppress("ktlint:standard:max-line-length")

package bose.ankush.commonui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import bose.ankush.commonui.constants.WeatherIconConstants
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawSun(
    animationProgress: Float,
    scale: Float = 1.0f,
    offsetX: Float = 0f,
    sunColor: Color,
    sunGlowColor: Color,
) {
    val centerX = size.width / 2 + offsetX
    val centerY = size.height / 2
    val radius = size.width.coerceAtMost(size.height) * 0.25f * scale

    val glowRadius =
        radius * (1.0f + WeatherIconConstants.SUN_PULSE_SCALE * sin(animationProgress * PI).toFloat())
    drawCircle(
        color = sunGlowColor,
        radius = glowRadius * WeatherIconConstants.SUN_GLOW_SCALE,
        center = Offset(centerX, centerY),
    )

    drawCircle(
        color = sunColor,
        radius = radius * (1.0f + WeatherIconConstants.SUN_BODY_VARIATION * sin(animationProgress * PI * 2).toFloat()),
        center = Offset(centerX, centerY),
    )

    val rayCount = 8

    for (i in 0 until rayCount) {
        val rayFactor = 0.8f + 0.2f * sin((animationProgress * PI * 2 + i).toFloat())
        val rayLength = radius * 0.6f * rayFactor

        val angle = (i * 360f / rayCount + animationProgress * 20f) * PI / 180f
        val startRadius = radius + 2f
        val endRadius = startRadius + rayLength

        val startX = centerX + cos(angle).toFloat() * startRadius
        val startY = centerY + sin(angle).toFloat() * startRadius
        val endX = centerX + cos(angle).toFloat() * endRadius
        val endY = centerY + sin(angle).toFloat() * endRadius

        val strokeWidth = 2f + 1f * sin((animationProgress * PI * 2 + i * 0.5f).toFloat())

        drawLine(
            color = sunColor.copy(alpha = 0.7f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
        )
    }
}

fun DrawScope.drawClouds(
    animationProgress: Float,
    cloudiness: Float,
    cloudColor: Color,
) {
    val cloudCount = (2 + (cloudiness * 2).toInt()).coerceAtMost(4)

    for (i in 0 until cloudCount) {
        val speedFactor = 0.8f + (i % 3) * 0.1f
        val baseX =
            size.width *
                (0.3f + (i * 0.15f) + animationProgress * WeatherIconConstants.CLOUD_MOVEMENT_SCALE * speedFactor) %
                size.width
        val baseY =
            size.height * (0.4f + (i % 2) * 0.1f + sin(animationProgress * PI * speedFactor) * 0.02f)

        val puffCount = 3
        val puffRadius = size.width * 0.1f

        for (j in 0 until puffCount) {
            val puffX = baseX + (j - 1) * (puffRadius * 1.2f)
            val puffY = baseY + sin((j + animationProgress * 1.5f) * PI).toFloat() * 2f
            val puffSize =
                puffRadius * (0.8f + (j % 2) * 0.4f + sin(animationProgress * PI + j) * 0.05f)

            val alpha =
                WeatherIconConstants.CLOUD_BASE_ALPHA + 0.2f * sin((animationProgress * PI + j * 0.5f).toFloat())

            drawCircle(
                color = cloudColor.copy(alpha = alpha),
                radius = puffSize.toFloat(),
                center = Offset(puffX, puffY.toFloat()),
            )
        }
    }
}

fun DrawScope.drawRain(
    animationProgress: Float,
    intensity: Float,
    rainColor: Color,
) {
    val baseDropCount = 8 + (intensity * 25).toInt()
    val dropCount = baseDropCount.coerceAtMost(30)

    val windStrength = sin(animationProgress * PI * 0.3f) * 0.2f + 0.1f

    for (i in 0 until dropCount) {
        val seed = (i * 13 + 7) % dropCount
        val speedFactor = 0.6f + (seed % 7) * 0.08f + intensity * 0.3f
        val phase = (seed * 0.1f) % 1.0f
        val uniqueProgress = (animationProgress * speedFactor + phase) % 1.0f
        val horizontalSeed = (seed * 17 + 3) % dropCount
        val dropThickness = 1.0f + intensity * 1.2f * (0.7f + (seed % 5) * 0.1f)
        val windEffect = windStrength * (1.5f - dropThickness * 0.3f)
        val initialX = size.width * ((horizontalSeed * 0.1f) % 1.0f)
        val dropX = initialX + sin(animationProgress * PI * 0.2f + seed) * size.width * 0.05f
        val dropY = size.height * (0.3f + uniqueProgress * 0.7f)
        val lengthVariation = 0.7f + (seed % 5) * 0.1f + speedFactor * 0.3f
        val dropLength = size.height * (0.05f + 0.08f * intensity) * lengthVariation
        val endX = dropX + windEffect * dropLength
        val endY = dropY + dropLength

        val baseAlpha =
            (WeatherIconConstants.RAIN_BASE_ALPHA - 0.2f + 0.4f * (dropThickness / 3.0f))
                .coerceIn(0.3f, 0.9f)
        val alphaVariation = 0.15f * sin((animationProgress * PI * 0.7f + seed).toFloat())
        val dropAlpha = (baseAlpha + alphaVariation).coerceIn(0.2f, 0.95f)

        drawLine(
            color = rainColor.copy(alpha = dropAlpha),
            start = Offset(dropX.toFloat(), dropY),
            end = Offset(endX.toFloat(), endY),
            strokeWidth = dropThickness,
        )

        if (endY >= size.height * 0.95f && seed % 3 == 0) {
            val splashProgress = (uniqueProgress * 3f) % 1.0f

            if (splashProgress < 0.3f) {
                val splashSize = size.width * 0.02f * (1f - splashProgress / 0.3f) * intensity
                val splashAlpha = (0.7f - splashProgress / 0.3f * 0.7f) * intensity * 0.8f

                drawCircle(
                    color = rainColor.copy(alpha = splashAlpha),
                    radius = splashSize,
                    center =
                        Offset(
                            endX.toFloat(),
                            size.height * 0.98f,
                        ),
                )

                if (intensity > 0.6f && seed % 6 == 0) {
                    val rippleProgress = splashProgress * 1.5f
                    if (rippleProgress < 0.3f) {
                        val rippleSize = splashSize * 2f * (rippleProgress / 0.3f)
                        val rippleAlpha = (0.3f - rippleProgress / 0.3f * 0.3f) * intensity * 0.5f

                        drawCircle(
                            color = rainColor.copy(alpha = rippleAlpha),
                            radius = rippleSize,
                            center =
                                Offset(
                                    endX.toFloat(),
                                    size.height * 0.98f,
                                ),
                        )
                    }
                }
            }
        }
    }
}

fun DrawScope.drawSnow(
    animationProgress: Float,
    intensity: Float,
    snowColor: Color,
) {
    val flakeCount = (5 + (intensity * 15).toInt()).coerceAtMost(20)

    for (i in 0 until flakeCount) {
        val speedFactor = 0.6f + (i % 5) * 0.1f
        val horizontalMovement =
            sin((animationProgress + i * 0.1f) * PI * 2) * size.width * WeatherIconConstants.SNOW_HORIZONTAL_MOVEMENT
        val flakeX = size.width * ((i * 0.1f) % 1.0f) + horizontalMovement
        val flakeProgress = (animationProgress * speedFactor + (i * 0.1f)) % 1.0f
        val flakeY = size.height * (0.5f + flakeProgress * 0.5f)

        val flakeSize = size.width * (0.015f + 0.01f * (i % 3) / 3f)

        drawCircle(
            color =
                snowColor.copy(
                    alpha =
                        WeatherIconConstants.SNOW_BASE_ALPHA + 0.2f * sin((animationProgress * PI + i).toFloat()),
                ),
            radius = flakeSize,
            center = Offset(flakeX.toFloat(), flakeY),
        )
    }
}

fun DrawScope.drawThunder(
    animationProgress: Float,
    thunderColor: Color,
) {
    val flashIntensity = sin(animationProgress * PI * 2).toFloat().coerceIn(0f, 1f)

    if (flashIntensity > 0.2f) {
        val centerX = size.width * 0.5f
        val startY = size.height * 0.4f

        val path =
            Path().apply {
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
            color = thunderColor.copy(alpha = flashIntensity * WeatherIconConstants.THUNDER_FLASH_ALPHA),
        )

        drawCircle(
            color = thunderColor.copy(alpha = flashIntensity * 0.3f),
            radius = size.width * 0.2f,
            center = Offset(centerX, startY + size.height * 0.2f),
        )
    }
}

fun DrawScope.drawFog(
    animationProgress: Float,
    fogColor: Color,
) {
    val layerCount = 6

    for (i in 0 until layerCount) {
        val layerY = size.height * (0.3f + i * 0.1f)
        val layerWidth = size.width * (0.6f + (i % 3) * 0.1f)
        val speedFactor = 0.8f + (i % 3) * 0.1f
        val layerOffset =
            size.width * 0.15f + sin((animationProgress * speedFactor + i * 0.2f) * PI).toFloat() * size.width * 0.08f

        val alpha =
            WeatherIconConstants.FOG_BASE_ALPHA + 0.2f * sin((animationProgress * PI + i * 0.5f)).toFloat()

        drawLine(
            color = fogColor.copy(alpha = alpha),
            start = Offset(layerOffset, layerY),
            end = Offset(layerOffset + layerWidth, layerY),
            strokeWidth = size.height * (0.02f + 0.01f * (i % 3) / 3f),
        )
    }
}

fun mapToWeatherCondition(description: String?): WeatherCondition {
    if (description.isNullOrBlank()) return WeatherCondition.CLEAR_SKY

    val lowerDesc = description.lowercase()

    WeatherCondition.entries.forEach { condition ->
        if (lowerDesc == condition.description.lowercase()) {
            return condition
        }
    }

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
        else -> WeatherCondition.CLEAR_SKY
    }
}
