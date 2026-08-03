package bose.ankush.language.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** In-memory override for the app's language, set from [bose.ankush.language.presentation.LanguageScreen]. */
var customAppLocale: String? by mutableStateOf(null)

/**
 * Bridges [customAppLocale] into the platform locale used by `stringResource()` (Compose Multiplatform
 * has no public API for this - https://github.com/JetBrains/compose-multiplatform/issues/4197).
 */
expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): Array<ProvidedValue<*>>
}

/** Wrap the app root with this so [customAppLocale] changes take effect without restarting the app. */
@Composable
fun AppEnvironment(content: @Composable () -> Unit) {
    CompositionLocalProvider(*(LocalAppLocale provides customAppLocale)) {
        key(customAppLocale) {
            content()
        }
    }
}
