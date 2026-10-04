package bose.ankush.home.presentation.wander

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Full-bleed home background. A dark gradient shows first. When [photoUrl] is set, Coil
 * hotlinks that URL (the one Unsplash returned). Otherwise Coil loads `drawable/wander_<key>`
 * if that resource exists. A bottom scrim keeps type readable.
 */
enum class WanderCondition(
    val key: String,
    val line: String,
) {
    CLEAR("clear", "It's clear"),
    CLOUDS("clouds", "It's cloudy"),
    RAIN("rain", "It's raining"),
    FOG("fog", "It's foggy"),
    STORM("storm", "It's stormy"),
    SNOW("snow", "It's snowing"),
    NIGHT("night", "It's night"),
    ;

    val gradient: Brush
        get() =
            when (this) {
                CLEAR -> vertical(clearTop, clearBottom)
                CLOUDS -> vertical(cloudsTop, cloudsBottom)
                RAIN -> vertical(rainTop, rainBottom)
                FOG -> vertical(fogTop, fogBottom)
                STORM -> vertical(stormTop, stormBottom)
                SNOW -> vertical(snowTop, snowBottom)
                NIGHT -> vertical(nightTop, nightBottom)
            }
}

@Composable
fun WanderConditionBackground(
    condition: WanderCondition,
    photoUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = condition to photoUrl,
            animationSpec = tween(durationMillis = BACKGROUND_FADE_MILLIS),
            label = "wanderConditionBackground",
        ) { (current, url) ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(current.gradient),
            ) {
                val bundled =
                    remember(current, context) {
                        bundledConditionRequest(context, current)
                    }
                val model = url ?: bundled
                if (model != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(readabilityScrim),
        )
    }
}

private fun bundledConditionRequest(
    context: Context,
    condition: WanderCondition,
): ImageRequest? {
    val drawable = bundledConditionDrawable(context, condition) ?: return null
    val uri = "android.resource://${drawable.packageName}/${drawable.resourceId}"
    return ImageRequest
        .Builder(context)
        .data(uri)
        .crossfade(true)
        .build()
}

internal data class BundledConditionDrawable(
    val packageName: String,
    val resourceId: Int,
)

internal fun bundledConditionDrawable(
    context: Context,
    condition: WanderCondition,
): BundledConditionDrawable? {
    val name = "wander_${condition.key}"
    val packages = listOf(context.packageName, HOME_RESOURCE_PACKAGE).distinct()
    for (packageName in packages) {
        val id = context.resources.getIdentifier(name, "drawable", packageName)
        if (id != 0) {
            return BundledConditionDrawable(packageName, id)
        }
    }
    return null
}

private fun vertical(
    top: Color,
    bottom: Color,
): Brush = Brush.verticalGradient(colors = listOf(top, bottom))

private const val HOME_RESOURCE_PACKAGE = "bose.ankush.home"
private const val BACKGROUND_FADE_MILLIS = 450

private val clearTop = Color(0xFF1E5A9A)
private val clearBottom = Color(0xFF071018)
private val cloudsTop = Color(0xFF6A7682)
private val cloudsBottom = Color(0xFF12161C)
private val rainTop = Color(0xFF3C5870)
private val rainBottom = Color(0xFF0B1218)
private val fogTop = Color(0xFF8B949C)
private val fogBottom = Color(0xFF1A1E24)
private val stormTop = Color(0xFF3A3358)
private val stormBottom = Color(0xFF0C0A14)
private val snowTop = Color(0xFF7E92A6)
private val snowBottom = Color(0xFF101820)
private val nightTop = Color(0xFF1A2440)
private val nightBottom = Color(0xFF05060C)

private val readabilityScrim =
    Brush.verticalGradient(
        colorStops =
            arrayOf(
                0f to Color.Transparent,
                0.38f to Color.Transparent,
                1f to Color(0xF0101014),
            ),
    )
