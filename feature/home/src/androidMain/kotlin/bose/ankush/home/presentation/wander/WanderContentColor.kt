package bose.ankush.home.presentation.wander

import android.content.Context
import android.graphics.Bitmap
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
import kotlin.math.pow

/** Soft off-white for dark photos. Not pure white. */
internal val WanderOnDark = Color(0xFFE7E4DC)

/** Muted ink for light photos. Not pure black. */
internal val WanderOnLight = Color(0xFF1C2430)

/**
 * Home text color. First paint uses the condition gradient. A photo, when one is
 * showing, is sampled after that and the color updates. The sample never blocks paint.
 */
@Composable
internal fun rememberWanderContentColor(
    condition: WanderCondition,
    photoUrl: String?,
): Color {
    val context = LocalContext.current
    val fallback = contentColorForLuminance(condition.dominantBackgroundLuminance())
    val photoModel =
        photoUrl ?: remember(condition, context) {
            bundledPhotoModel(context, condition)
        }
    var color by remember(condition, photoModel) { mutableStateOf(fallback) }
    LaunchedEffect(condition, photoModel) {
        color = fallback
        if (photoModel == null) return@LaunchedEffect
        val sampled = sampleAverageRelativeLuminance(context, photoModel)
        color = contentColorForLuminance(sampled ?: condition.dominantBackgroundLuminance())
    }
    return color
}

internal fun contentColorForLuminance(luminance: Double): Color =
    if (luminance < WANDER_LUMINANCE_THRESHOLD) WanderOnDark else WanderOnLight

/**
 * Mean Rec. 709 relative luminance of [bitmap]. Channels are linearized sRGB,
 * then weighted 0.2126 / 0.7152 / 0.0722.
 */
internal fun averageRelativeLuminance(bitmap: Bitmap): Double {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return 0.0
    var total = 0.0
    val row = IntArray(width)
    for (y in 0 until height) {
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        for (pixel in row) {
            total += relativeLuminance(pixel)
        }
    }
    return total / (width * height)
}

internal fun relativeLuminance(color: Color): Double =
    REC709_RED * linearSrgb(color.red.toDouble()) +
        REC709_GREEN * linearSrgb(color.green.toDouble()) +
        REC709_BLUE * linearSrgb(color.blue.toDouble())

private fun relativeLuminance(argb: Int): Double {
    val red = ((argb shr RED_SHIFT) and CHANNEL_MASK) / BYTE_MAX
    val green = ((argb shr GREEN_SHIFT) and CHANNEL_MASK) / BYTE_MAX
    val blue = (argb and CHANNEL_MASK) / BYTE_MAX
    return REC709_RED * linearSrgb(red) + REC709_GREEN * linearSrgb(green) + REC709_BLUE * linearSrgb(blue)
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
private suspend fun sampleAverageRelativeLuminance(
    context: Context,
    model: Any,
): Double? =
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
        averageRelativeLuminance(
            image.toBitmap(width = SAMPLE_EDGE_PX, height = SAMPLE_EDGE_PX),
        )
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
