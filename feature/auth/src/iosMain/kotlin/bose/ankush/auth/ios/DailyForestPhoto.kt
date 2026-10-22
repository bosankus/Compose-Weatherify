package bose.ankush.auth.ios

import bose.ankush.network.api.UnsplashApi
import bose.ankush.network.model.UnsplashPhoto
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSUserDefaults
import kotlin.time.Clock

/** The sign-in backdrop for one day. */
internal class ForestPhoto(
    val imageUrl: String,
    val photographer: String,
    val profileUrl: String,
    val downloadLocation: String,
)

/**
 * A different Unsplash forest for each day, the same for everyone on that day: the date picks
 * one of a few calm forest searches and one photo from its results. The day's pick is kept, so
 * relaunching the same day costs no API call. Offline or without a key there is no photo, and
 * the screen shows its gradient instead.
 */
internal class DailyForestPhoto(
    private val api: UnsplashApi,
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    private var tracked = false

    suspend fun today(): ForestPhoto? {
        val day = epochDay()
        cached(day)?.let { return it }
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
        photo?.let { store(day, it) }
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

    /** Today's stored pick, if one was stored today with a URL and a credit. */
    private fun cached(day: Int): ForestPhoto? {
        val fields = FIELDS.map { defaults.stringForKey(KEY_PREFIX + it).orEmpty() }
        val usable =
            defaults
                .integerForKey(KEY_DAY)
                .toInt() == day && fields[0].isNotBlank() && fields[1].isNotBlank()
        return if (usable) ForestPhoto(fields[0], fields[1], fields[2], fields[3]) else null
    }

    private fun store(
        day: Int,
        photo: ForestPhoto,
    ) {
        defaults.setInteger(day.toLong(), forKey = KEY_DAY)
        listOf(photo.imageUrl, photo.photographer, photo.profileUrl, photo.downloadLocation)
            .zip(FIELDS)
            .forEach { (value, field) -> defaults.setObject(value, forKey = KEY_PREFIX + field) }
    }

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
        const val KEY_DAY = "login_forest.day"
        const val KEY_PREFIX = "login_forest."
        val FIELDS = listOf("url", "name", "profile", "download")
        const val REFERRAL = "utm_source=weatherify&utm_medium=referral"
    }
}
