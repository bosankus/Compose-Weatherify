package bose.ankush.home.presentation.screen

import android.content.Context

/** A persisted pick and the day it was picked for; null when that day is unknown. */
internal data class PersistedPhoto(
    val photo: BackgroundPhoto,
    val epochDay: Int?,
)

/**
 * Last shown Unsplash photo, keyed by [BackdropSlot.key] (and a global last). SharedPreferences
 * so a cold start can hotlink Coil's disk cache before weather or network is ready.
 */
internal class BackgroundPhotoPreferences(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The slot's own pick, else the last photo shown anywhere (marked stale so it is replaced). */
    fun read(slotKey: String): PersistedPhoto? =
        readKey(KEY_PREFIX + slotKey) ?: readKey(KEY_LAST)?.copy(epochDay = null)

    fun write(
        slotKey: String,
        epochDay: Int,
        photo: BackgroundPhoto,
    ) {
        prefs
            .edit()
            .putString(KEY_PREFIX + slotKey, encode(photo, epochDay))
            .putString(KEY_LAST, encode(photo, epochDay))
            .apply()
    }

    private fun readKey(key: String): PersistedPhoto? = prefs.getString(key, null)?.let(::decode)

    private fun encode(
        photo: BackgroundPhoto,
        epochDay: Int,
    ): String =
        listOf(
            photo.id,
            photo.imageUrl,
            photo.photographer,
            photo.profileUrl,
            photo.downloadLocation,
            epochDay.toString(),
        ).joinToString(SEP)

    private fun decode(raw: String): PersistedPhoto? {
        val parts = raw.split(SEP)
        val url = parts.getOrNull(1)?.takeIf { parts.size >= 5 && it.isNotBlank() }
        return url?.let {
            PersistedPhoto(
                photo =
                    BackgroundPhoto(
                        id = parts[0],
                        imageUrl = it,
                        photographer = parts[2],
                        profileUrl = parts[3],
                        downloadLocation = parts[4],
                    ),
                // Entries written before daily picks have no day, so they count as stale.
                epochDay = parts.getOrNull(5)?.toIntOrNull(),
            )
        }
    }

    private companion object {
        const val PREFS = "background_photo"
        const val KEY_PREFIX = "photo_"
        const val KEY_LAST = "photo_last"
        const val SEP = "\u001f"
    }
}

/** Live Unsplash URL, else persisted Coil cache URL, else null to load the bundled drawable. */
internal fun chooseBackgroundPhotoUrl(
    liveUrl: String?,
    persistedUrl: String?,
): String? = liveUrl?.takeIf { it.isNotBlank() } ?: persistedUrl?.takeIf { it.isNotBlank() }

internal fun bundledDrawableNames(condition: SkyCondition): List<String> =
    listOf("background_${condition.key}", "background_default")
