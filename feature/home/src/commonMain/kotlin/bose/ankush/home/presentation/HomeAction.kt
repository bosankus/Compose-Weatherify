package bose.ankush.home.presentation

import bose.ankush.home.domain.model.HomeWeather

internal sealed interface HomeAction {
    /**
     * A new [HomeWeather]. [message] is the text for its failure or missing location, already
     * resolved from string resources, or null when it has neither.
     */
    data class WeatherChanged(
        val weather: HomeWeather,
        val message: String?,
    ) : HomeAction

    data class Error(
        val message: String?,
    ) : HomeAction

    data class UpdateNotificationBanner(
        val show: Boolean = false,
        val isPermanentlyDeclined: Boolean? = null,
        val resetDismissal: Boolean = false,
    ) : HomeAction

    data object DismissNotificationBanner : HomeAction

    /** "Reset to GPS" tapped while [name] was pinned. */
    data class ResetLocationStarted(
        val name: String,
    ) : HomeAction

    /** The override could not be cleared; the chip goes back to its idle look. */
    data object ResetLocationFailed : HomeAction
}
