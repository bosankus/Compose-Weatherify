package bose.ankush.auth.backdrop

import bose.ankush.network.api.UnsplashApi
import bose.ankush.network.model.UnsplashPhoto
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** The sign-in backdrop for one day. */
internal class ForestPhoto(
    val imageUrl: String,
    val photographer: String,
    val profileUrl: String,
    val downloadLocation: String,
)

/** Keeps the day's pick on the device: NSUserDefaults on iOS, SharedPreferences on Android. */
internal interface ForestPhotoStore {
    /** The pick stored for [day], or null when none was stored that day. */
    fun load(day: Int): ForestPhoto?

    fun save(
        day: Int,
        photo: ForestPhoto,
    )
}

/**
 * A different Unsplash forest for each day, the same for everyone on that day: the date picks
 * one of a few calm forest searches and one photo from its results. The day's pick is kept, so
 * relaunching the same day costs no API call. Offline or without a key there is no photo, and
 * the screen shows its gradient instead.
 */
internal class DailyForestPhoto(
    private val api: UnsplashApi,
    private val store: ForestPhotoStore,
) {
    private var tracked = false

    suspend fun today(): ForestPhoto? {
        val day = epochDay()
        store.load(day)?.let { return it }
        val photo =
            try {
                val results =
                    api.searchPhotos(query = QUERIES[day % QUERIES.size], perPage = PER_PAGE)
                results
                    .takeIf { it.isNotEmpty() }
                    ?.let { it[(day / QUERIES.size) % it.size] }
                    ?.toForestPhoto()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        photo?.let { store.save(day, it) }
        return photo
    }

    /** Unsplash asks for one download ping per photo that is actually shown. */
    suspend fun trackShown(photo: ForestPhoto) {
        if (tracked || photo.downloadLocation.isBlank()) return
        tracked = true
        try {
            api.trackDownload(photo.downloadLocation)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Attempted once; not retried.
        }
    }

    private fun epochDay(): Int =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toEpochDays()
            .toInt()

    private fun UnsplashPhoto.toForestPhoto(): ForestPhoto =
        ForestPhoto(
            imageUrl = urls.raw + (if ('?' in urls.raw) "&" else "?") + "w=1170&h=2532&fit=crop&fm=jpg&q=80",
            photographer = user.name,
            profileUrl = user.links.html + (if ('?' in user.links.html) "&" else "?") + REFERRAL,
            downloadLocation = links.downloadLocation,
        )

    private companion object {
        val QUERIES =
            listOf(
                "misty forest",
                "green forest",
                "pine forest",
                "forest path",
                "moss forest",
                "rainforest canopy",
                "forest morning light",
            )
        const val PER_PAGE = 30
        const val REFERRAL = "utm_source=weatherify&utm_medium=referral"
    }
}

/** Field names shared by both platform stores, so a stored pick reads back the same way. */
internal val FOREST_FIELDS = listOf("url", "name", "profile", "download")

internal fun ForestPhoto.fieldValues(): List<String> = listOf(imageUrl, photographer, profileUrl, downloadLocation)

/** A stored pick is usable only with a URL and a credit. */
internal fun forestPhotoOrNull(fields: List<String>): ForestPhoto? =
    if (fields[0].isNotBlank() && fields[1].isNotBlank()) {
        ForestPhoto(fields[0], fields[1], fields[2], fields[3])
    } else {
        null
    }
