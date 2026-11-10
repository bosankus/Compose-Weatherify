package bose.ankush.home.domain.model

import bose.ankush.home.domain.location.LocationProblem
import bose.ankush.home.domain.nearby.GeoPoint

/**
 * The place the shown forecast belongs to. It changes only when that place's forecast lands,
 * so anything keyed on it (the header, nearby events) moves together with the forecast.
 */
internal data class ForecastPlace(
    /** The pinned name, else the reverse-geocoded one. Null until a name is known. */
    val name: String?,
    val isPinned: Boolean,
    /** For handing the place to other features. Nothing in home decides with it. */
    val point: GeoPoint,
)

/** Everything home shows about the weather, as one value. Built by `ObserveHomeWeather`. */
internal data class HomeWeather(
    val snapshot: WeatherSnapshot? = null,
    val place: ForecastPlace? = null,
    /** False until the first Room read finishes, with or without a row. */
    val cacheChecked: Boolean = false,
    val status: Status = Status.Pending,
    /** A network refresh is in flight. Separate from [status]: a cached forecast is often settled first. */
    val isFetching: Boolean = false,
    /** Why GPS was not used on the latest attempt, if anything went wrong. */
    val locationProblem: LocationProblem? = null,
) {
    sealed interface Status {
        /** An attempt is under way and has not settled. */
        data object Pending : Status

        /** The forecast for [place] is the latest home could get. */
        data object Ready : Status

        /** The attempt failed. [snapshot], if any, is still the last saved one. */
        data class Failed(
            val cause: Throwable,
        ) : Status

        /** No pinned place, no GPS fix, and no saved spot. [locationProblem] says why, if known. */
        data object NoLocation : Status
    }
}
