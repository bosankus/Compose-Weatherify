package bose.ankush.home.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import bose.ankush.network.api.UnsplashApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide cache, keyed by the Unsplash query for the live condition.
 * A blank key or a failed search is not cached, so the gradient stays.
 */
internal object BackgroundPhotoCache {
    private val photos = ConcurrentHashMap<String, BackgroundPhoto>()
    private val inflight = ConcurrentHashMap<String, CompletableDeferred<BackgroundPhoto?>>()
    private val trackedIds = mutableSetOf<String>()
    private val mutex = Mutex()

    /**
     * Where downloads run. A Compose effect that restarts cancels its coroutine on the main thread,
     * and Ktor's Android engine closes the connection inside that cancel handler, so a download
     * started in the effect crashes with NetworkOnMainThreadException. Downloads started here are
     * never cancelled by a screen: the caller only waits for the result.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun cached(query: String): BackgroundPhoto? = photos[query]

    suspend fun photo(
        query: String,
        api: UnsplashApi,
    ): BackgroundPhoto? {
        photos[query]?.let { return it }
        val request = reserve(query)
        if (request.leader) scope.launch { load(query, request.pending, api) }
        return request.pending.await()
    }

    /**
     * One download ping per photo id, after that photo is on screen.
     * The same id is not pinged again in this process.
     */
    suspend fun trackShown(
        photo: BackgroundPhoto,
        api: UnsplashApi,
    ) {
        val firstTime = mutex.withLock { trackedIds.add(photo.id) }
        if (!firstTime) return
        scope.launch {
            try {
                api.trackDownload(photo.downloadLocation)
            } catch (_: Exception) {
                // The ping was attempted. Do not call trackDownload again for this id.
            }
        }
    }

    private fun reserve(query: String): LoadRequest {
        val ready = photos[query]
        if (ready != null) return LoadRequest(CompletableDeferred(ready), leader = false)
        val created = CompletableDeferred<BackgroundPhoto?>()
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
        pending: CompletableDeferred<BackgroundPhoto?>,
        api: UnsplashApi,
    ) {
        var loaded: BackgroundPhoto? = null
        try {
            loaded =
                try {
                    api.searchPhotos(query = query, perPage = 1).firstOrNull()?.toBackgroundPhoto()
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
        val pending: CompletableDeferred<BackgroundPhoto?>,
        val leader: Boolean,
    )
}

/**
 * Offline-first backdrop photo. Shows the last persisted URL for [condition] (Coil disk/memory)
 * immediately, then searches Unsplash for [query] and crossfades to the new photo. A blank key
 * or a failed search keeps the persisted or bundled image.
 */
@Composable
fun rememberBackgroundPhoto(
    query: String?,
    condition: SkyCondition = SkyCondition.CLOUDS,
): BackgroundPhoto? {
    val api = koinInject<UnsplashApi>()
    val context = LocalContext.current
    val prefs = remember { BackgroundPhotoPreferences(context) }
    var photo by remember(condition.key) {
        mutableStateOf(
            prefs.read(condition.key) ?: query?.let(BackgroundPhotoCache::cached),
        )
    }
    LaunchedEffect(query, condition.key) {
        prefs.read(condition.key)?.let { photo = it }
        if (query == null) return@LaunchedEffect
        BackgroundPhotoCache.cached(query)?.let {
            photo = it
            prefs.write(condition.key, it)
            return@LaunchedEffect
        }
        for (waitMs in PHOTO_SEARCH_BACKOFF_MS) {
            if (waitMs > 0L) delay(waitMs)
            BackgroundPhotoCache.cached(query)?.let {
                photo = it
                prefs.write(condition.key, it)
                return@LaunchedEffect
            }
            val result = BackgroundPhotoCache.photo(query, api)
            if (result != null) {
                photo = result
                prefs.write(condition.key, result)
                return@LaunchedEffect
            }
        }
    }
    return photo
}

/** Runs only from the branch that actually puts the photo on screen. */
@Composable
internal fun TrackShownBackgroundPhoto(photo: BackgroundPhoto) {
    val api = koinInject<UnsplashApi>()
    LaunchedEffect(photo.id) {
        BackgroundPhotoCache.trackShown(photo, api)
    }
}

private val PHOTO_SEARCH_BACKOFF_MS = longArrayOf(0L, 2_000L, 5_000L, 15_000L)
