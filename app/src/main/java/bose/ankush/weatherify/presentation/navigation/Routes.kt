package bose.ankush.weatherify.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

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
