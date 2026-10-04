package bose.ankush.home.presentation.wander

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import bose.ankush.network.api.UnsplashApi
import bose.ankush.network.model.UnsplashPhoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.util.concurrent.ConcurrentHashMap

data class WanderFogPhoto(
    val id: String,
    val imageUrl: String,
    val photographer: String,
    val profileUrl: String,
    val downloadLocation: String,
)

/**
 * Process-wide cache, not a screen `remember`. Fog is searched once per process
 * and only when that condition has no cached photo. Other conditions never search.
 * Leaving the shell and opening it again reads this cache.
 */
internal object WanderConditionPhotoCache {
    private val photos = ConcurrentHashMap<WanderCondition, WanderFogPhoto>()
    private val trackedIds = mutableSetOf<String>()
    private var fogLoad: CompletableDeferred<WanderFogPhoto?>? = null
    private val mutex = Mutex()

    fun cached(condition: WanderCondition): WanderFogPhoto? = photos[condition]

    suspend fun photo(
        condition: WanderCondition,
        api: UnsplashApi,
    ): WanderFogPhoto? {
        if (condition != WanderCondition.FOG) return null
        return photos[condition] ?: sharedFogPhoto(api)
    }

    private suspend fun sharedFogPhoto(api: UnsplashApi): WanderFogPhoto? {
        val request = mutex.withLock { reserveFogLoad() }
        if (request.leader) loadFog(request.pending, api)
        return request.pending.await()
    }

    /**
     * One download ping per photo id, after that photo is on screen.
     * The same id is not pinged again in this process.
     */
    suspend fun trackShown(
        photo: WanderFogPhoto,
        api: UnsplashApi,
    ) {
        val firstTime = mutex.withLock { trackedIds.add(photo.id) }
        if (!firstTime) return
        try {
            api.trackDownload(photo.downloadLocation)
        } catch (cancelled: CancellationException) {
            mutex.withLock { trackedIds.remove(photo.id) }
            throw cancelled
        } catch (_: Exception) {
            // The ping was attempted. Do not call trackDownload again for this id.
        }
    }

    private fun reserveFogLoad(): FogRequest {
        val ready = photos[WanderCondition.FOG]
        val inFlight = fogLoad
        val pending: CompletableDeferred<WanderFogPhoto?>
        val leader: Boolean
        when {
            ready != null -> {
                pending = CompletableDeferred(ready)
                leader = false
            }
            inFlight != null -> {
                pending = inFlight
                leader = false
            }
            else -> {
                pending = CompletableDeferred()
                fogLoad = pending
                leader = true
            }
        }
        return FogRequest(pending, leader)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun loadFog(
        pending: CompletableDeferred<WanderFogPhoto?>,
        api: UnsplashApi,
    ) {
        var loaded: WanderFogPhoto? = null
        try {
            loaded =
                try {
                    api.searchPhotos(query = FOG_LANDSCAPE_QUERY, perPage = 1).firstOrNull()?.toWanderPhoto()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Blank key or a failed search keeps the gradient. Do not cache a miss.
                    null
                }
        } finally {
            withContext(NonCancellable) {
                val published =
                    mutex.withLock {
                        if (loaded != null) photos.putIfAbsent(WanderCondition.FOG, loaded)
                        if (fogLoad === pending) fogLoad = null
                        photos[WanderCondition.FOG]
                    }
                if (!pending.isCompleted) pending.complete(published)
            }
        }
    }

    private data class FogRequest(
        val pending: CompletableDeferred<WanderFogPhoto?>,
        val leader: Boolean,
    )
}

/**
 * Searches only while the shown condition is fog. The client already asks Unsplash
 * for a portrait photo, content_filter high, and per_page 1. A blank key or any
 * failure keeps the gradient. The image URL is the raw URL Unsplash returned, plus
 * the existing size params.
 */
@Composable
fun rememberWanderFogPhoto(condition: WanderCondition): WanderFogPhoto? {
    val api = koinInject<UnsplashApi>()
    var photo by remember(condition) { mutableStateOf(WanderConditionPhotoCache.cached(condition)) }
    LaunchedEffect(condition) {
        photo = WanderConditionPhotoCache.photo(condition, api)
    }
    return if (condition == WanderCondition.FOG) photo else null
}

/** Runs only from the branch that actually puts the photo on screen. */
@Composable
internal fun TrackShownWanderPhoto(photo: WanderFogPhoto) {
    val api = koinInject<UnsplashApi>()
    LaunchedEffect(photo.id) {
        WanderConditionPhotoCache.trackShown(photo, api)
    }
}

private fun UnsplashPhoto.toWanderPhoto(): WanderFogPhoto =
    WanderFogPhoto(
        id = id,
        imageUrl = unsplashBackgroundUrl(urls.raw),
        photographer = user.name,
        profileUrl = unsplashReferralUrl(user.links.html),
        downloadLocation = links.downloadLocation,
    )

internal fun unsplashBackgroundUrl(raw: String): String {
    val joiner = if ('?' in raw) "&" else "?"
    return raw + joiner + "w=1080&h=1920&fit=crop&fm=jpg&dpr=2"
}

internal fun unsplashReferralUrl(profileHtml: String): String {
    val joiner = if ('?' in profileHtml) "&" else "?"
    return profileHtml + joiner + "utm_source=weatherify&utm_medium=referral"
}

internal const val UNSPLASH_HOME_URL = "https://unsplash.com/?utm_source=weatherify&utm_medium=referral"

private const val FOG_LANDSCAPE_QUERY = "fog landscape"
