package bose.ankush.home.presentation.wander

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Soft off-white for dark photos. Not pure white. */
internal val WanderOnDark = Color(0xFFE7E4DC)

/** Muted ink for light photos. Not pure black. */
internal val WanderOnLight = Color(0xFF1C2430)

/** Surface and content colors for the refreshing chip, derived from the background. */
internal data class WanderChipColors(
    val surface: Color,
    val content: Color,
)

/**
 * Home text color. First paint uses the condition gradient. A photo, when one is
 * showing, is sampled after that and the color updates. The sample never blocks paint.
 */
@Composable
internal fun rememberWanderContentColor(
    condition: WanderCondition,
    photoUrl: String?,
): Color {
    val sample = rememberWanderImageSample(condition, photoUrl)
    return contentColorForLuminance(sample.headerLuminance)
}

/**
 * Animates chip colors for a background average. Pair with [rememberWanderImageSample]
 * so the photo is decoded once for text and chip alike.
 */
@Composable
internal fun rememberWanderChipColors(averageColor: Color): WanderChipColors {
    val target = wanderChipColors(averageColor)
    val surface by animateColorAsState(
        targetValue = target.surface,
        animationSpec = tween(durationMillis = CHIP_COLOR_ANIM_MS),
        label = "wanderChipSurface",
    )
    val content by animateColorAsState(
        targetValue = target.content,
        animationSpec = tween(durationMillis = CHIP_COLOR_ANIM_MS),
        label = "wanderChipContent",
    )
    return WanderChipColors(surface = surface, content = content)
}

@Composable
internal fun rememberWanderImageSample(
    condition: WanderCondition,
    photoUrl: String?,
): WanderImageSample {
    val context = LocalContext.current
    val fallback =
        WanderImageSample(
            luminance = condition.dominantBackgroundLuminance(),
            headerLuminance = condition.dominantHeaderLuminance(),
            averageColor = condition.dominantBackgroundColor(),
        )
    val photoModel =
        photoUrl ?: remember(condition, context) {
            bundledPhotoModel(context, condition)
        }
    var sample by remember(condition, photoModel) { mutableStateOf(fallback) }
    LaunchedEffect(condition, photoModel) {
        sample = fallback
        if (photoModel == null) return@LaunchedEffect
        val decoded = sampleImage(context, photoModel)
        if (decoded != null) sample = decoded
    }
    return sample
}

internal fun contentColorForLuminance(luminance: Double): Color =
    if (luminance < WANDER_LUMINANCE_THRESHOLD) WanderOnDark else WanderOnLight

/**
 * Builds translucent chip colors from an opaque average background color.
 * Content is [WanderOnDark] or [WanderOnLight]. Surface is that average, darkened
 * or lightened, at [CHIP_SURFACE_ALPHA], then nudged until WCAG contrast is at least 4.5:1
 * against the surface composited over the average.
 */
internal fun wanderChipColors(averageColor: Color): WanderChipColors {
    val opaque = averageColor.copy(alpha = 1f)
    val luminance = relativeLuminance(opaque)
    val darkBackground = luminance < WANDER_LUMINANCE_THRESHOLD
    val content = if (darkBackground) WanderOnDark else WanderOnLight
    var factor = CHIP_TINT_START
    var surfaceOpaque = if (darkBackground) darken(opaque, factor) else lighten(opaque, factor)
    var surface = surfaceOpaque.copy(alpha = CHIP_SURFACE_ALPHA)
    var steps = 0
    while (steps < CHIP_TINT_MAX_STEPS &&
        contrastRatio(content, compositeOver(surface, opaque)) < WCAG_AA_CONTRAST
    ) {
        factor = (factor + CHIP_TINT_STEP).coerceAtMost(CHIP_TINT_MAX)
        surfaceOpaque = if (darkBackground) darken(opaque, factor) else lighten(opaque, factor)
        surface = surfaceOpaque.copy(alpha = CHIP_SURFACE_ALPHA)
        steps++
    }
    return WanderChipColors(surface = surface, content = content)
}

/** WCAG contrast ratio of two opaque (or already-composited) colors. */
internal fun contrastRatio(
    foreground: Color,
    background: Color,
): Double {
    val l1 = relativeLuminance(foreground.copy(alpha = 1f))
    val l2 = relativeLuminance(background.copy(alpha = 1f))
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + CONTRAST_OFFSET) / (darker + CONTRAST_OFFSET)
}

/** Alpha-composites [foreground] over opaque [background]. */
internal fun compositeOver(
    foreground: Color,
    background: Color,
): Color {
    val a = foreground.alpha
    val inv = 1f - a
    return Color(
        red = foreground.red * a + background.red * inv,
        green = foreground.green * a + background.green * inv,
        blue = foreground.blue * a + background.blue * inv,
        alpha = 1f,
    )
}

internal fun darken(
    color: Color,
    factor: Float,
): Color {
    val t = factor.coerceIn(0f, 1f)
    return Color(
        red = (color.red * (1f - t)).coerceIn(CHANNEL_FLOOR, CHANNEL_CEILING),
        green = (color.green * (1f - t)).coerceIn(CHANNEL_FLOOR, CHANNEL_CEILING),
        blue = (color.blue * (1f - t)).coerceIn(CHANNEL_FLOOR, CHANNEL_CEILING),
        alpha = 1f,
    )
}

internal fun lighten(
    color: Color,
    factor: Float,
): Color {
    val t = factor.coerceIn(0f, 1f)
    return Color(
        red = (color.red + (1f - color.red) * t).coerceIn(CHANNEL_FLOOR, CHANNEL_CEILING),
        green = (color.green + (1f - color.green) * t).coerceIn(CHANNEL_FLOOR, CHANNEL_CEILING),
        blue = (color.blue + (1f - color.blue) * t).coerceIn(CHANNEL_FLOOR, CHANNEL_CEILING),
        alpha = 1f,
    )
}

/**
 * Mean Rec. 709 relative luminance of [bitmap]. Channels are linearized sRGB,
 * then weighted 0.2126 / 0.7152 / 0.0722.
 */
internal fun averageRelativeLuminance(bitmap: Bitmap): Double = sampleBitmap(bitmap).luminance

internal fun relativeLuminance(color: Color): Double =
    REC709_RED * linearSrgb(color.red.toDouble()) +
        REC709_GREEN * linearSrgb(color.green.toDouble()) +
        REC709_BLUE * linearSrgb(color.blue.toDouble())

internal data class WanderImageSample(
    val luminance: Double,
    val headerLuminance: Double,
    val averageColor: Color,
)

internal fun sampleBitmap(bitmap: Bitmap): WanderImageSample {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) {
        return WanderImageSample(
            luminance = 0.0,
            headerLuminance = 0.0,
            averageColor = Color.Black,
        )
    }
    val headerHeight = headerSampleHeight(height)
    var totalLuminance = 0.0
    var headerLuminanceTotal = 0.0
    var totalRed = 0.0
    var totalGreen = 0.0
    var totalBlue = 0.0
    val row = IntArray(width)
    val count = (width * height).toDouble()
    val headerCount = (width * headerHeight).toDouble()
    for (y in 0 until height) {
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        for (pixel in row) {
            val red = ((pixel shr RED_SHIFT) and CHANNEL_MASK) / BYTE_MAX
            val green = ((pixel shr GREEN_SHIFT) and CHANNEL_MASK) / BYTE_MAX
            val blue = (pixel and CHANNEL_MASK) / BYTE_MAX
            val pixelLuminance =
                REC709_RED * linearSrgb(red) +
                    REC709_GREEN * linearSrgb(green) +
                    REC709_BLUE * linearSrgb(blue)
            totalLuminance += pixelLuminance
            if (y < headerHeight) {
                headerLuminanceTotal += pixelLuminance
            }
            totalRed += red
            totalGreen += green
            totalBlue += blue
        }
    }
    return WanderImageSample(
        luminance = totalLuminance / count,
        headerLuminance = headerLuminanceTotal / headerCount,
        averageColor =
            Color(
                red = (totalRed / count).toFloat(),
                green = (totalGreen / count).toFloat(),
                blue = (totalBlue / count).toFloat(),
                alpha = 1f,
            ),
    )
}

/**
 * How many rows from the top of [imageHeight] represent the header text band.
 * Matches the upper ~45% of a portrait, ContentScale.Crop background.
 */
internal fun headerSampleHeight(
    imageHeight: Int,
    fraction: Float = HEADER_LUMINANCE_FRACTION,
): Int {
    if (imageHeight <= 0) return 0
    return (imageHeight * fraction).toInt().coerceIn(1, imageHeight)
}

private fun linearSrgb(channel: Double): Double {
    val s = channel.coerceIn(0.0, 1.0)
    return if (s <= SRGB_LINEAR_THRESHOLD) {
        s / SRGB_LINEAR_SLOPE
    } else {
        ((s + SRGB_OFFSET) / SRGB_SCALE).pow(SRGB_GAMMA)
    }
}

@Suppress("TooGenericExceptionCaught", "SwallowedException")
private suspend fun sampleImage(
    context: Context,
    model: Any,
): WanderImageSample? =
    try {
        val result =
            context.imageLoader.execute(
                ImageRequest
                    .Builder(context)
                    .data(model)
                    .size(SAMPLE_EDGE_PX, SAMPLE_EDGE_PX)
                    .allowHardware(false)
                    .build(),
            )
        val image = (result as? SuccessResult)?.image ?: return null
        if (image.width <= 0 || image.height <= 0) return null
        sampleBitmap(image.toBitmap(width = SAMPLE_EDGE_PX, height = SAMPLE_EDGE_PX))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

private fun bundledPhotoModel(
    context: Context,
    condition: WanderCondition,
): String? {
    val drawable = bundledConditionDrawable(context, condition) ?: return null
    return "android.resource://${drawable.packageName}/${drawable.resourceId}"
}

private const val WANDER_LUMINANCE_THRESHOLD = 0.45
private const val SAMPLE_EDGE_PX = 24
internal const val HEADER_LUMINANCE_FRACTION = 0.45f
private const val SRGB_LINEAR_THRESHOLD = 0.04045
private const val SRGB_LINEAR_SLOPE = 12.92
private const val SRGB_OFFSET = 0.055
private const val SRGB_SCALE = 1.055
private const val SRGB_GAMMA = 2.4
private const val REC709_RED = 0.2126
private const val REC709_GREEN = 0.7152
private const val REC709_BLUE = 0.0722
private const val BYTE_MAX = 255.0
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL_MASK = 0xFF
private const val CHIP_SURFACE_ALPHA = 0.60f
private const val CHIP_TINT_START = 0.22f
private const val CHIP_TINT_STEP = 0.08f
private const val CHIP_TINT_MAX = 0.72f
private const val CHIP_TINT_MAX_STEPS = 8
private const val WCAG_AA_CONTRAST = 4.5
private const val CONTRAST_OFFSET = 0.05
private const val CHANNEL_FLOOR = 0.05f
private const val CHANNEL_CEILING = 0.95f
private const val CHIP_COLOR_ANIM_MS = 150
