package bose.ankush.home.ios

import bose.ankush.home.presentation.screen.BackdropSlot
import bose.ankush.home.presentation.screen.BackgroundPhoto
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
 * iOS counterpart of the Android `rememberBackgroundPhoto`: shows the photo persisted for the
 * slot (condition and part of the day) straight away. If that pick is from an earlier day, it
 * searches Unsplash for today's (with backoff) and persists it. A blank key or a failed search
 * keeps what is shown.
 */
internal class IosBackdropPhotos(
    private val api: UnsplashApi,
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    private val cache = mutableMapOf<String, BackgroundPhoto>()
    private val tracked = mutableSetOf<String>()
    private val mutex = Mutex()

    /**
     * Emits only photos, never a null for a slot with nothing stored, so the photo on screen stays
     * until the next part of the day has one of its own.
     */
    fun photos(slot: BackdropSlot): Flow<BackgroundPhoto> =
        flow {
            val persisted = read(slot.key)
            val cached = slot.cacheKey?.let { mutex.withLock { cache[it] } }
            (persisted?.first ?: cached)?.let { emit(it) }
            // Today's pick is already stored: no search, so a relaunch costs no API call.
            if (persisted?.second == slot.epochDay) return@flow
            if (slot.cacheKey == null) return@flow
            for (waitMs in SEARCH_BACKOFF_MS) {
                if (waitMs > 0L) delay(waitMs)
                val found = search(slot)
                if (found != null) {
                    write(slot.key, slot.epochDay, found)
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

    private suspend fun search(slot: BackdropSlot): BackgroundPhoto? {
        val cacheKey = slot.cacheKey ?: return null
        val query = slot.searchQuery ?: return null
        mutex.withLock { cache[cacheKey] }?.let { return it }
        val found =
            try {
                api
                    .searchPhotos(query = query, perPage = BackdropSlot.RESULTS_PER_SEARCH)
                    .let(slot::pick)
                    ?.toBackgroundPhoto()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        if (found != null) mutex.withLock { cache[cacheKey] = found }
        return found
    }

    /** The slot's stored photo and the day it was picked for (null when unknown). */
    private fun read(slotKey: String): Pair<BackgroundPhoto, Int?>? {
        val fields = FIELDS.map { defaults.stringForKey(key(slotKey, it)) }
        // The download link may be blank; the rest must be there to draw and credit the photo.
        if (fields.take(DOWNLOAD_INDEX).any { it.isNullOrBlank() }) return null
        val photo =
            BackgroundPhoto(
                id = fields[0].orEmpty(),
                imageUrl = fields[1].orEmpty(),
                photographer = fields[2].orEmpty(),
                profileUrl = fields[3].orEmpty(),
                downloadLocation = fields[DOWNLOAD_INDEX].orEmpty(),
            )
        return photo to defaults.stringForKey(key(slotKey, DAY_FIELD))?.toIntOrNull()
    }

    private fun write(
        slotKey: String,
        epochDay: Int,
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
            .forEach { (field, value) -> defaults.setObject(value, key(slotKey, field)) }
        defaults.setObject(epochDay.toString(), key(slotKey, DAY_FIELD))
    }

    private fun key(
        slotKey: String,
        field: String,
    ) = "background_photo.$slotKey.$field"

    private companion object {
        val FIELDS = listOf("id", "url", "photographer", "profile", "download")
        const val DOWNLOAD_INDEX = 4
        const val DAY_FIELD = "day"
        val SEARCH_BACKOFF_MS = longArrayOf(0L, 2_000L, 5_000L, 15_000L)
    }
}
