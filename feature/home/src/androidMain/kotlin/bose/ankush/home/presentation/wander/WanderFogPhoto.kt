package bose.ankush.home.presentation.wander

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import bose.ankush.network.api.UnsplashApi
import org.koin.compose.koinInject

data class WanderFogPhoto(
    val imageUrl: String,
    val photographer: String,
    val profileUrl: String,
)

/**
 * One live search for a foggy London landscape. The image URL is the raw URL
 * Unsplash returned, plus resize params. Nothing is hardcoded and nothing is bundled.
 */
@Composable
fun rememberWanderFogPhoto(): WanderFogPhoto? {
    val api = koinInject<UnsplashApi>()
    var photo by remember { mutableStateOf<WanderFogPhoto?>(null) }
    LaunchedEffect(api) {
        val found =
            runCatching { api.searchPhotos(query = "foggy London", perPage = 1).firstOrNull() }
                .getOrNull() ?: return@LaunchedEffect
        runCatching { api.trackDownload(found.links.downloadLocation) }
        photo =
            WanderFogPhoto(
                imageUrl = unsplashBackgroundUrl(found.urls.raw),
                photographer = found.user.name,
                profileUrl = unsplashReferralUrl(found.user.links.html),
            )
    }
    return photo
}

internal fun unsplashBackgroundUrl(raw: String): String {
    val joiner = if ('?' in raw) "&" else "?"
    return raw + joiner + "w=1080&h=1920&fit=crop&fm=jpg&dpr=2"
}

internal fun unsplashReferralUrl(profileHtml: String): String {
    val joiner = if ('?' in profileHtml) "&" else "?"
    return profileHtml + joiner + "utm_source=weatherify&utm_medium=referral"
}

internal const val UNSPLASH_HOME_URL = "https://unsplash.com/?utm_source=weatherify&utm_medium=referral"
