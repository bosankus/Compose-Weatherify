package bose.ankush.auth.backdrop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bose.ankush.auth.generated.resources.Res
import bose.ankush.auth.generated.resources.login_photo_credit
import bose.ankush.network.api.UnsplashApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** Today's forest behind the sign-in screen, with Unsplash's required credit. */
data class LoginPhotoUi(
    val imageUrl: String,
    val credit: String,
    val profileUrl: String,
    val unsplashUrl: String,
)

/**
 * Loads the day's forest once and reports when it is actually on screen, which Unsplash
 * counts as a download. Null until it loads, and for good when offline.
 */
class LoginBackdropViewModel internal constructor(
    api: UnsplashApi,
    store: ForestPhotoStore,
) : ViewModel() {
    private val forest = DailyForestPhoto(api, store)
    private var today: ForestPhoto? = null
    private val _photo = MutableStateFlow<LoginPhotoUi?>(null)
    val photo: StateFlow<LoginPhotoUi?> = _photo.asStateFlow()

    init {
        viewModelScope.launch {
            val picked = forest.today() ?: return@launch
            today = picked
            _photo.value = picked.toUi()
        }
    }

    /** Call once the photo is actually on screen. */
    fun photoShown() {
        val shown = today ?: return
        viewModelScope.launch { forest.trackShown(shown) }
    }
}

internal suspend fun ForestPhoto.toUi(): LoginPhotoUi =
    LoginPhotoUi(
        imageUrl = imageUrl,
        credit = getString(Res.string.login_photo_credit, photographer),
        profileUrl = profileUrl,
        unsplashUrl = UNSPLASH_URL,
    )

internal const val UNSPLASH_URL = "https://unsplash.com/?utm_source=weatherify&utm_medium=referral"
