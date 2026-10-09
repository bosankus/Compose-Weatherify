package bose.ankush.home.domain.location

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