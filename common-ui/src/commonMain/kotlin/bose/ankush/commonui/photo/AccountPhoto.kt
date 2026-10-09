package bose.ankush.commonui.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest

/**
 * The account photo's cache key. The server signs a new URL on every fetch, so the full URL
 * changes while the photo does not; the path before the query names the stored object.
 */
fun accountPhotoCacheKey(photoUrl: String): String =
    ACCOUNT_PHOTO_KEY_PREFIX + photoUrl.substringBefore('?')

/**
 * Loads the account photo once and serves it from memory and disk after that, until logout
 * clears it. A re-signed URL for the same photo is a cache hit, not a download.
 */
@Composable
fun rememberAccountPhotoRequest(photoUrl: String): ImageRequest {
    val context = LocalPlatformContext.current
    return remember(context, photoUrl) {
        val key = accountPhotoCacheKey(photoUrl)
        ImageRequest
            .Builder(context)
            .data(photoUrl)
            .memoryCacheKey(key)
            .diskCacheKey(key)
            .build()
    }
}

/** Writes and drops the account photo in [imageLoader]'s caches, under [accountPhotoCacheKey]. */
class AccountPhotoCache(
    private val imageLoader: ImageLoader,
) {
    /**
     * Puts just-uploaded [bytes] in the disk cache and drops the old photo from memory, so the
     * next load of [photoUrl] reads them from disk. Empty metadata marks a hand-written entry,
     * which Coil serves as is. Failing here only costs one download later.
     */
    @Suppress("TooGenericExceptionCaught")
    fun store(
        photoUrl: String,
        bytes: ByteArray,
    ) {
        val key = accountPhotoCacheKey(photoUrl)
        removeFromMemory(key)
        val diskCache = imageLoader.diskCache ?: return
        val editor = diskCache.openEditor(key) ?: return
        try {
            diskCache.fileSystem.write(editor.metadata) {}
            diskCache.fileSystem.write(editor.data) { write(bytes) }
            editor.commit()
        } catch (_: Exception) {
            editor.abort()
            diskCache.remove(key)
        }
    }

    fun remove(photoUrl: String) {
        val key = accountPhotoCacheKey(photoUrl)
        removeFromMemory(key)
        imageLoader.diskCache?.remove(key)
    }

    /** Every cached image, on logout. */
    fun clear() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    private fun removeFromMemory(key: String) {
        val memoryCache = imageLoader.memoryCache ?: return
        // A request's extras are part of its memory key, so match on the string alone.
        memoryCache.keys.filter { it.key == key }.forEach(memoryCache::remove)
    }
}

private const val ACCOUNT_PHOTO_KEY_PREFIX = "account-photo:"
