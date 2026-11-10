package bose.ankush.routing

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the Weatherify app.
 *
 * All feature modules should define their own routes here to keep the routing
 * module as the single source of truth for navigation keys. Feature modules
 * then self-register their navigation graphs via [NavGraphBuilder] extensions.
 */
@Serializable
data object HomeRoute : NavKey

@Serializable
data object SavedLocationsRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data class LanguageRoute(
    val languages: List<String>,
) : NavKey