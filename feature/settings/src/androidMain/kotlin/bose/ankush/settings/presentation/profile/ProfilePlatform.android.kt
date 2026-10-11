package bose.ankush.settings.presentation.profile

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import bose.ankush.settings.domain.ProfilePhoto
import bose.ankush.settings.presentation.profile.ProfilePhotoEncoder.MAX_EDGE_PX
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

@Composable
internal actual fun rememberProfilePhotoPicker(
    onPicked: (ProfilePhoto) -> Unit,
    onUnreadable: () -> Unit,
): PhotoPickerLauncher? {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val latestOnPicked by rememberUpdatedState(onPicked)
    val latestOnUnreadable by rememberUpdatedState(onUnreadable)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val photo =
                    withContext(Dispatchers.IO) { ProfilePhotoEncoder.encodeOrNull(resolver, uri) }
                if (photo != null) latestOnPicked(photo) else latestOnUnreadable()
            }
        }
    return remember(launcher) {
        PhotoPickerLauncher {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

/**
 * Decodes whatever the picker returned (JPEG, PNG, WebP, HEIC…) with [ImageDecoder], which also
 * applies the EXIF rotation, scales the longer edge down to [MAX_EDGE_PX], and re-encodes as
 * JPEG. That keeps uploads small and never sends HEIC, which the server rejects.
 */
internal object ProfilePhotoEncoder {
    private const val MAX_EDGE_PX = 1080
    private const val JPEG_QUALITY = 85

    @Suppress("TooGenericExceptionCaught")
    fun encodeOrNull(
        resolver: ContentResolver,
        uri: Uri,
    ): ProfilePhoto? =
        try {
            encode(resolver, uri)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    private fun encode(
        resolver: ContentResolver,
        uri: Uri,
    ): ProfilePhoto? {
        val bitmap =
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(
                    resolver,
                    uri
                )
            ) { decoder, info, _ ->
                // Software memory: hardware bitmaps cannot be compressed.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val scale = MAX_EDGE_PX.toFloat() / maxOf(width, height)
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (width * scale).roundToInt().coerceAtLeast(1),
                        (height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }
        return try {
            val out = ByteArrayOutputStream()
            val bytes = if (bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    JPEG_QUALITY,
                    out
                )
            ) out.toByteArray() else null
            bytes?.takeIf { it.isNotEmpty() && it.size <= ProfilePhoto.MAX_BYTES }
                ?.let(::ProfilePhoto)
        } finally {
            bitmap.recycle()
        }
    }
}
