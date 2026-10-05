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
import kotlinx.coroutines.delay
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
 * Process-wide cache, keyed by the Unsplash query for the live condition.
 * A blank key or a failed search is not cached, so the gradient stays.
 */
internal object WanderConditionPhotoCache {
    private val photos = ConcurrentHashMap<String, WanderFogPhoto>()
    private val inflight = ConcurrentHashMap<String, CompletableDeferred<WanderFogPhoto?>>()
    private val trackedIds = mutableSetOf<String>()
    private val mutex = Mutex()

    fun cached(query: String): WanderFogPhoto? = photos[query]

    suspend fun photo(
        query: String,
        api: UnsplashApi,
    ): WanderFogPhoto? {
        photos[query]?.let { return it }
        val request = reserve(query)
        if (request.leader) load(query, request.pending, api)
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

    private fun reserve(query: String): LoadRequest {
        val ready = photos[query]
        if (ready != null) return LoadRequest(CompletableDeferred(ready), leader = false)
        val created = CompletableDeferred<WanderFogPhoto?>()
        val existing = inflight.putIfAbsent(query, created)
        return if (existing == null) {
            LoadRequest(created, leader = true)
        } else {
            LoadRequest(existing, leader = false)
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun load(
        query: String,
        pending: CompletableDeferred<WanderFogPhoto?>,
        api: UnsplashApi,
    ) {
        var loaded: WanderFogPhoto? = null
        try {
            loaded =
                try {
                    api.searchPhotos(query = query, perPage = 1).firstOrNull()?.toWanderPhoto()
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
                        if (loaded != null) photos.putIfAbsent(query, loaded)
                        if (inflight[query] === pending) inflight.remove(query)
                        photos[query]
                    }
                if (!pending.isCompleted) pending.complete(published)
            }
        }
    }

    private data class LoadRequest(
        val pending: CompletableDeferred<WanderFogPhoto?>,
        val leader: Boolean,
    )
}

/**
 * Searches for the live condition. The client already asks Unsplash for a portrait
 * photo, content_filter high, and per_page 1. A null query, a blank key, or any
 * failure keeps the gradient. The image URL is the raw URL Unsplash returned, plus
 * the existing size params.
 */
@Composable
fun rememberWanderConditionPhoto(query: String?): WanderFogPhoto? {
    val api = koinInject<UnsplashApi>()
    var photo by remember(query) { mutableStateOf(query?.let(WanderConditionPhotoCache::cached)) }
    LaunchedEffect(query) {
        if (query == null) {
            photo = null
            return@LaunchedEffect
        }
        // Cached hit from remember above: still re-check so a concurrent fill is picked up.
        WanderConditionPhotoCache.cached(query)?.let {
            photo = it
            return@LaunchedEffect
        }
        // Cold start often races the network. Retry a miss with short backoff while the query
        // is unchanged. Failures are not cached, so each attempt re-searches.
        for (waitMs in PHOTO_SEARCH_BACKOFF_MS) {
            if (waitMs > 0L) delay(waitMs)
            WanderConditionPhotoCache.cached(query)?.let {
                photo = it
                return@LaunchedEffect
            }
            val result = WanderConditionPhotoCache.photo(query, api)
            if (result != null) {
                photo = result
                return@LaunchedEffect
            }
        }
    }
    return photo
}

/** Runs only from the branch that actually puts the photo on screen. */
@Composable
internal fun TrackShownWanderPhoto(photo: WanderFogPhoto) {
    val api = koinInject<UnsplashApi>()
    LaunchedEffect(photo.id) {
        WanderConditionPhotoCache.trackShown(photo, api)
    }
}

/** OpenWeather `weather.main` to an Unsplash search. Unknown mains use one default. */
internal fun unsplashQuery(weatherMain: String): String =
    when (weatherMain.lowercase()) {
        "clear" -> "clear sky landscape"
        "clouds" -> "clouds landscape"
        "rain", "drizzle" -> "rain landscape"
        "snow" -> "snow landscape"
        "thunderstorm", "squall", "tornado" -> "thunderstorm landscape"
        "mist" -> "mist landscape"
        "fog" -> "fog landscape"
        else -> "weather landscape"
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

private val PHOTO_SEARCH_BACKOFF_MS = longArrayOf(0L, 2_000L, 5_000L, 15_000L)
