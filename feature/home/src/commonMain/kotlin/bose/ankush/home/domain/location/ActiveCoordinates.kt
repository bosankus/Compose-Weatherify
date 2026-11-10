package bose.ankush.home.domain.location

import bose.ankush.home.domain.nearby.GeoPoint
import bose.ankush.home.domain.nearby.distanceMetersTo

internal data class ActiveCoordinates(
    val coordinates: Coordinates,
    val isOverridden: Boolean = false,
    val overrideName: String? = null,
)

internal sealed interface LocationProblem {
    data object PermissionDenied : LocationProblem
    data class GpsDisabled(val cause: Throwable) : LocationProblem
    data class Failed(val cause: Throwable) : LocationProblem
}

internal data class CoordinateResult(
    val coordinates: ActiveCoordinates?,
    val problem: LocationProblem? = null,
)

internal val ActiveCoordinates.point: GeoPoint
    get() = GeoPoint(coordinates.latitude, coordinates.longitude)

/**
 * Whether a forecast fetched at [fetchedAt] is this place's forecast: the exact pinned spot, or
 * for GPS anything within [GPS_SAME_PLACE_METERS]. A row with no point never is.
 */
internal fun ActiveCoordinates.isServedBy(fetchedAt: GeoPoint?): Boolean =
    when {
        fetchedAt == null -> false
        isOverridden -> fetchedAt == point
        else -> fetchedAt.distanceMetersTo(point) <= GPS_SAME_PLACE_METERS
    }

/**
 * Two GPS fixes this close share a forecast: jitter, not a move. An assumption in the same
 * spirit as [bose.ankush.home.domain.usecase.SAVED_PLACE_MATCH_METERS], not a contract value.
 */
internal const val GPS_SAME_PLACE_METERS = 1_000.0
