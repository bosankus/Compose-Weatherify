package bose.ankush.home.ios

import bose.ankush.home.presentation.screen.BackgroundPhoto
import bose.ankush.home.presentation.screen.SkyCondition
import bose.ankush.home.presentation.screen.toBackgroundPhoto
import bose.ankush.network.api.UnsplashApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSUserDefaults

/**
 * iOS counterpart of the Android `rememberBackgroundPhoto`: shows the photo last persisted for
 * the condition straight away, then searches Unsplash for the live query (with backoff) and
 * persists the result. A blank key or a failed search keeps the persisted photo or the gradient.
 */
internal class IosBackdropPhotos(
    private val api: UnsplashApi,
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    private val cache = mutableMapOf<String, BackgroundPhoto>()
    private val tracked = mutableSetOf<String>()
    private val mutex = Mutex()

    fun photos(
        condition: SkyCondition,
        query: String?,
    ): Flow<BackgroundPhoto?> =
        flow {
            val persisted = read(condition.key)
            emit(persisted ?: query?.let { mutex.withLock { cache[it] } })
            if (query == null) return@flow
            for (waitMs in SEARCH_BACKOFF_MS) {
                if (waitMs > 0L) delay(waitMs)
                val found = search(query)
                if (found != null) {
                    write(condition.key, found)
                    emit(found)
                    return@flow
                }
            }
        }

    /** One Unsplash download ping per photo id, once that photo is on screen. */
    suspend fun trackShown(photo: BackgroundPhoto) {
        if (!mutex.withLock { tracked.add(photo.id) }) return
        try {
            api.trackDownload(photo.downloadLocation)
        } catch (cancelled: CancellationException) {
            mutex.withLock { tracked.remove(photo.id) }
            throw cancelled
        } catch (_: Exception) {
            // The ping was attempted; do not repeat it for this id.
        }
    }

    private suspend fun search(query: String): BackgroundPhoto? {
        mutex.withLock { cache[query] }?.let { return it }
        val found =
            try {
                api.searchPhotos(query = query, perPage = 1).firstOrNull()?.toBackgroundPhoto()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        if (found != null) mutex.withLock { cache[query] = found }
        return found
    }

    private fun read(conditionKey: String): BackgroundPhoto? {
        val fields = FIELDS.map { defaults.stringForKey(key(conditionKey, it)) }
        // The download link may be blank; the rest must be there to draw and credit the photo.
        if (fields.take(DOWNLOAD_INDEX).any { it.isNullOrBlank() }) return null
        return BackgroundPhoto(
            id = fields[0].orEmpty(),
            imageUrl = fields[1].orEmpty(),
            photographer = fields[2].orEmpty(),
            profileUrl = fields[3].orEmpty(),
            downloadLocation = fields[DOWNLOAD_INDEX].orEmpty(),
        )
    }

    private fun write(
        conditionKey: String,
        photo: BackgroundPhoto,
    ) {
        val values = listOf(
            photo.id,
            photo.imageUrl,
            photo.photographer,
            photo.profileUrl,
            photo.downloadLocation
        )
        FIELDS.zip(values)
            .forEach { (field, value) -> defaults.setObject(value, key(conditionKey, field)) }
    }

    private fun key(
        conditionKey: String,
        field: String,
    ) = "background_photo.$conditionKey.$field"

    private companion object {
        val FIELDS = listOf("id", "url", "photographer", "profile", "download")
        const val DOWNLOAD_INDEX = 4
        val SEARCH_BACKOFF_MS = longArrayOf(0L, 2_000L, 5_000L, 15_000L)
    }
}
