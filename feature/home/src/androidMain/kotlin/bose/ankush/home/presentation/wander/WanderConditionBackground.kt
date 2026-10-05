package bose.ankush.home.presentation.wander

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-bleed home background. A dark gradient shows first. When [photoUrl] is set, Coil
 * hotlinks that URL (the one Unsplash returned). Otherwise Coil loads `drawable/wander_<key>`
 * if that resource exists. Soft top and bottom scrims keep type readable.
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
                if (url != null) {
                    RetryingAsyncImage(url = url, modifier = Modifier.fillMaxSize())
                } else if (bundled != null) {
                    AsyncImage(
                        model = bundled,
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
                    .background(topReadabilityScrim),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(readabilityScrim),
        )
    }
}

/**
 * Reloads a failed Unsplash hotlink a few times. Cold start and flaky networks often
 * miss the first Coil attempt; bumping a retry key after a short delay asks again.
 */
@Composable
private fun RetryingAsyncImage(
    url: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var attempt by remember(url) { mutableIntStateOf(0) }
    val model =
        remember(url, attempt) {
            ImageRequest
                .Builder(context)
                .data(url)
                .crossfade(true)
                .build()
        }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        onError = {
            if (attempt < IMAGE_LOAD_MAX_RETRIES) {
                val retryAt = attempt
                scope.launch {
                    delay(IMAGE_LOAD_RETRY_DELAY_MS)
                    if (attempt == retryAt) attempt = retryAt + 1
                }
            }
        },
    )
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
    val names = bundledDrawableNames(condition)
    val packages = listOf(context.packageName, HOME_RESOURCE_PACKAGE).distinct()
    for (name in names) {
        for (packageName in packages) {
            val id = context.resources.getIdentifier(name, "drawable", packageName)
            if (id != 0) {
                return BundledConditionDrawable(packageName, id)
            }
        }
    }
    return null
}

internal fun WanderCondition.dominantBackgroundLuminance(): Double {
    val (top, bottom) = dominantGradientEnds()
    return (relativeLuminance(top) + relativeLuminance(bottom)) / 2
}

/** Mean of the condition gradient ends. Used before a photo is sampled. */
internal fun WanderCondition.dominantBackgroundColor(): Color {
    val (top, bottom) = dominantGradientEnds()
    return Color(
        red = (top.red + bottom.red) / 2f,
        green = (top.green + bottom.green) / 2f,
        blue = (top.blue + bottom.blue) / 2f,
        alpha = 1f,
    )
}

/** Top-of-gradient luminance for header text before a photo is sampled. */
internal fun WanderCondition.dominantHeaderLuminance(): Double = relativeLuminance(dominantGradientEnds().first)

private fun WanderCondition.dominantGradientEnds(): Pair<Color, Color> =
    when (this) {
        WanderCondition.CLEAR -> clearTop to clearBottom
        WanderCondition.CLOUDS -> cloudsTop to cloudsBottom
        WanderCondition.RAIN -> rainTop to rainBottom
        WanderCondition.FOG -> fogTop to fogBottom
        WanderCondition.STORM -> stormTop to stormBottom
        WanderCondition.SNOW -> snowTop to snowBottom
        WanderCondition.NIGHT -> nightTop to nightBottom
    }

private fun vertical(
    top: Color,
    bottom: Color,
): Brush = Brush.verticalGradient(colors = listOf(top, bottom))

private const val HOME_RESOURCE_PACKAGE = "bose.ankush.home"
private const val BACKGROUND_FADE_MILLIS = 450
private const val IMAGE_LOAD_MAX_RETRIES = 3
private const val IMAGE_LOAD_RETRY_DELAY_MS = 1_500L

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

private val topReadabilityScrim =
    Brush.verticalGradient(
        colorStops =
            arrayOf(
                0f to Color(0x55101418),
                0.22f to Color(0x22101418),
                0.40f to Color.Transparent,
                1f to Color.Transparent,
            ),
    )

private val readabilityScrim =
    Brush.verticalGradient(
        colorStops =
            arrayOf(
                0f to Color.Transparent,
                0.38f to Color.Transparent,
                1f to Color(0xF0101014),
            ),
    )
