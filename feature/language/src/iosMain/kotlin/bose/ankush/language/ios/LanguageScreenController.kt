package bose.ankush.language.ios

import bose.ankush.language.generated.resources.Res
import bose.ankush.language.generated.resources.language_navigate_back
import bose.ankush.language.generated.resources.language_screen_subtitle
import bose.ankush.language.generated.resources.language_screen_title
import bose.ankush.language.presentation.regionOf
import bose.ankush.language.util.LanguageCatalog
import bose.ankush.language.util.LocaleHelper.changeLanguageTo
import bose.ankush.language.util.LocaleHelper.getDefaultLanguage
import bose.ankush.language.util.LocaleHelper.getDisplayName
import bose.ankush.language.util.customAppLocale
import bose.ankush.language.util.matchLanguage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSUserDefaults

/** What the SwiftUI language page draws. */
data class LanguageScreenUi(
    val title: String,
    val subtitle: String,
    val backLabel: String,
    val rows: List<LanguageRowUi>,
)

/** One language: its own name in its own script, and the region its flag comes from. */
data class LanguageRowUi(
    val tag: String,
    val name: String,
    val region: String,
    val isSelected: Boolean,
)

/**
 * The SwiftUI language page's entry into shared code: the bundled catalog, the current pick
 * (same matching as the Compose screen), and applying a new one. Swift calls [close] when the
 * page goes away.
 */
class LanguageScreenController {
    private val scope = MainScope()
    private val _state = MutableStateFlow<LanguageScreenUi?>(null)

    /** Null until the catalog and copy are loaded. */
    val state: StateFlow<LanguageScreenUi?> = _state.asStateFlow()

    private var languages: List<String> = emptyList()

    init {
        scope.launch {
            languages =
                runCatching { LanguageCatalog.getAvailableLanguages() }.getOrDefault(emptyList())
            publish(matchLanguage(languages, customAppLocale ?: getDefaultLanguage()))
        }
    }

    /**
     * Applies [tag] app-wide: remembered for next launch, and the order iOS (and Compose's
     * resource lookup) reads its preferred languages from, so shared copy follows at once.
     */
    fun select(tag: String) {
        if (tag !in languages) return
        // The platform may echo the tag back in another form (iw-IL as he-IL).
        val applied = matchLanguage(languages, changeLanguageTo(tag)) ?: tag
        customAppLocale = tag
        NSUserDefaults.standardUserDefaults.setObject(listOf(tag), forKey = APPLE_LANGUAGES)
        scope.launch { publish(applied) }
    }

    fun close() = scope.cancel()

    private suspend fun publish(selected: String?) {
        _state.value =
            LanguageScreenUi(
                title = getString(Res.string.language_screen_title),
                subtitle = getString(Res.string.language_screen_subtitle),
                backLabel = getString(Res.string.language_navigate_back),
                rows =
                    languages.map { tag ->
                        LanguageRowUi(
                            tag = tag,
                            name = tag.getDisplayName(),
                            region = regionOf(tag),
                            isSelected = tag == selected,
                        )
                    },
            )
    }

    private companion object {
        const val APPLE_LANGUAGES = "AppleLanguages"
    }
}
