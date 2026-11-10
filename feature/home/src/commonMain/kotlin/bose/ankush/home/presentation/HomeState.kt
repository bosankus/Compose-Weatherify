package bose.ankush.home.presentation

import bose.ankush.home.domain.location.LocationProblem
import bose.ankush.home.domain.model.AirQuality
import bose.ankush.home.domain.model.ForecastPlace
import bose.ankush.home.domain.model.WeatherForecast
import bose.ankush.home.domain.model.WeatherSnapshot

internal data class HomeState(
    /**
     * Weather work is in progress: an attempt that has not settled, or a network refresh. The one
     * loading state: it drives the bottom loader on both platforms, with or without a forecast.
     */
    val isLoading: Boolean = true,
    /** False until the first Room read on this ViewModel finishes, with or without a row. */
    val hasCheckedCache: Boolean = false,
    /** The place [snapshot] belongs to. */
    val place: ForecastPlace? = null,
    val snapshot: WeatherSnapshot? = null,
    /** What went wrong on the latest attempt, if anything. */
    val notice: Notice? = null,
    /** Why GPS was not used on the latest attempt, if anything went wrong. */
    val locationProblem: LocationProblem? = null,
    val notificationBanner: NotificationBanner = NotificationBanner(),
    /**
     * The pinned place's name while Home switches back to GPS. Keeps the chip up, with a
     * spinner, until the GPS forecast has landed (or the attempt failed).
     */
    val resettingFrom: String? = null,
) {
    sealed interface Notice {
        /** Resolved from string resources; null when there is no text for it. */
        val message: String?

        /** Nothing to fall back on: the error page. */
        data class Error(
            override val message: String?,
        ) : Notice

        /** A forecast is still on screen, or the location cause is known: the offline notice. */
        data class Offline(
            override val message: String?,
        ) : Notice
    }

    data class NotificationBanner(
        val isVisible: Boolean = false,
        val isPermanentlyDeclined: Boolean = false,
        /** Sticks until a permission change resets it, so a re-check does not re-show the banner. */
        val isDismissed: Boolean = false,
    )

    val weatherData: WeatherForecast? get() = snapshot?.forecast
    val airQualityData: AirQuality? get() = snapshot?.airQuality

    val error: String? get() = (notice as? Notice.Error)?.message
    val isOffline: Boolean get() = notice is Notice.Offline
    val offlineMessage: String? get() = (notice as? Notice.Offline)?.message

    val isGpsDisabled: Boolean get() = locationProblem is LocationProblem.GpsDisabled
    val isLocationPermissionDenied: Boolean get() = locationProblem is LocationProblem.PermissionDenied

    /** The pinned place's name, for the "reset to current location" chip. */
    val pinnedPlaceName: String?
        get() = place?.takeIf { it.isPinned }?.name?.takeIf { it.isNotBlank() }

    /** What the location chip shows: the pinned place, or the one being reset away from. */
    val locationChipName: String? get() = pinnedPlaceName ?: resettingFrom

    val isResettingLocation: Boolean get() = resettingFrom != null
}
