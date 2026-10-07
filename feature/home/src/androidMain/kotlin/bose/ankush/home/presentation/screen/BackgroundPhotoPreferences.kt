package bose.ankush.home.presentation.screen

import android.content.Context

/**
 * Last shown Unsplash photo, keyed by condition (and a global last). SharedPreferences so a
 * cold start can hotlink Coil's disk cache before weather or network is ready.
 */
internal class BackgroundPhotoPreferences(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(conditionKey: String): BackgroundPhoto? =
        readKey(KEY_PREFIX + conditionKey) ?: readKey(KEY_LAST)

    fun write(
        conditionKey: String,
        photo: BackgroundPhoto,
    ) {
        prefs
            .edit()
            .putString(KEY_PREFIX + conditionKey, encode(photo))
            .putString(KEY_LAST, encode(photo))
            .apply()
    }

    private fun readKey(key: String): BackgroundPhoto? = prefs.getString(key, null)?.let(::decode)

    private fun encode(photo: BackgroundPhoto): String =
        listOf(
            photo.id,
            photo.imageUrl,
            photo.photographer,
            photo.profileUrl,
            photo.downloadLocation,
        ).joinToString(SEP)

    private fun decode(raw: String): BackgroundPhoto? {
        val parts = raw.split(SEP)
        val url = parts.getOrNull(1)?.takeIf { parts.size >= 5 && it.isNotBlank() }
        return url?.let {
            BackgroundPhoto(
                id = parts[0],
                imageUrl = it,
                photographer = parts[2],
                profileUrl = parts[3],
                downloadLocation = parts[4],
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
