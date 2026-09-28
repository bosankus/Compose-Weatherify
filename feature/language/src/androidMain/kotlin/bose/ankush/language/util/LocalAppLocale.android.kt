package bose.ankush.language.util

import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import java.util.Locale

actual object LocalAppLocale {
    private var default: Locale? = null

    actual val current: String
        @Composable
        get() = LocalLocale.current.platformLocale.toString()

    @Composable
    actual infix fun provides(value: String?): Array<ProvidedValue<*>> {
        val context = LocalContext.current
        if (default == null) {
            default = LocalLocale.current.platformLocale
        }
        val new =
            when (value) {
                null -> default!!
                else -> Locale.forLanguageTag(value)
            }
        Locale.setDefault(new)
        val configuration = Configuration(LocalConfiguration.current)
        configuration.setLocale(new)
        // The Activity must stay reachable through the Context chain: androidx lookups such as
        // LocalActivity and LocalActivityResultRegistryOwner walk ContextWrapper.baseContext, and
        // createConfigurationContext() on its own returns a context that no longer wraps the
        // Activity - which made rememberLauncherForActivityResult() crash. So wrap the Activity and
        // only delegate resources/assets to the localised context.
        val configuredContext =
            remember(context, configuration) {
                val localized = context.createConfigurationContext(configuration)
                object : ContextWrapper(context) {
                    override fun getResources(): Resources = localized.resources

                    override fun getAssets(): AssetManager = localized.assets
                }
            }
        return arrayOf(
            LocalConfiguration provides configuration,
            LocalContext provides configuredContext,
        )
    }
}
