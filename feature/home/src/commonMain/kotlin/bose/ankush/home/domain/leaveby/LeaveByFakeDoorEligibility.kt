package bose.ankush.home.domain.leaveby

/**
 * Saved place reduced to coordinates. Eligibility does not read the place name,
 * the coordinates, or the local time.
 */
internal data class LeaveByPlace(
    val lat: Double,
    val lon: Double,
)

/**
 * Preview gate: at least [MIN_SAVED_PLACES] saved places.
 * Time of day and metro bounding boxes are not checked.
 * The Remote Config flag is enforced by the caller, not here.
 */
internal object LeaveByFakeDoorEligibility {
    const val MIN_SAVED_PLACES = 2

    fun isEligible(places: List<LeaveByPlace>): Boolean = places.size >= MIN_SAVED_PLACES
}
