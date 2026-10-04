package bose.ankush.home.domain.leaveby

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month

/**
 * Saved place reduced to coordinates. Eligibility never reads the place name.
 */
internal data class LeaveByPlace(
    val lat: Double,
    val lon: Double,
)

/**
 * Axis-aligned municipal bounding box. Edges are inclusive.
 *
 * Numbers are OpenStreetMap administrative bounding boxes via the public Nominatim
 * search API (`format=jsonv2`, `boundingbox` = minLat, maxLat, minLon, maxLon),
 * fetched 2026-10-04. Licence: Data © OpenStreetMap contributors, ODbL 1.0.
 * https://nominatim.openstreetmap.org
 *
 * These are city / district extents, not the wider metropolitan regions
 * (Bengaluru BMR, Mumbai MMR, Delhi NCR, Hyderabad HMDA). An axis-aligned box can
 * still include a thin strip outside the true polygon.
 */
internal data class MetroBox(
    val name: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
) {
    fun contains(
        lat: Double,
        lon: Double,
    ): Boolean = lat in minLat..maxLat && lon in minLon..maxLon
}

/**
 * All four rules are required. The local-time window is the device timezone:
 * hour 16, 17, 18, or 19, i.e. 16:00:00 inclusive through 19:59:59 inclusive,
 * and 20:00:00 exclusive. Monsoon is June, July, August, or September in that
 * same local calendar.
 */
@Suppress("MagicNumber")
internal object LeaveByFakeDoorEligibility {
    const val MIN_SAVED_PLACES = 2

    /**
     * Bengaluru city relation 7902476.
     * https://nominatim.openstreetmap.org/search?q=Bengaluru&format=jsonv2&limit=1
     */
    val BENGALURU =
        MetroBox(
            name = "Bengaluru",
            minLat = 12.8334905,
            maxLat = 13.1426196,
            minLon = 77.4598797,
            maxLon = 77.7840639,
        )

    /**
     * Greater Mumbai as the union of the two districts that make up the city,
     * not the Mumbai Metropolitan Region:
     * Mumbai City District relation 7964376
     * (18.8921596, 19.0525850, 72.7916763, 72.9068144) and
     * Mumbai Suburban District relation 7964375
     * (18.9924937, 19.2694771, 72.7732289, 72.9817485).
     * https://nominatim.openstreetmap.org/search?q=Mumbai%20City%20District&countrycodes=in&format=jsonv2&limit=1
     * https://nominatim.openstreetmap.org/search?q=Mumbai%20Suburban%20District&countrycodes=in&format=jsonv2&limit=1
     */
    val MUMBAI =
        MetroBox(
            name = "Mumbai",
            minLat = 18.8921596,
            maxLat = 19.2694771,
            minLon = 72.7732289,
            maxLon = 72.9817485,
        )

    /**
     * Delhi city relation 21180767 (not the smaller New Delhi relation 21180662,
     * and not the National Capital Region).
     * https://nominatim.openstreetmap.org/search?city=Delhi&country=India&format=jsonv2&limit=3
     */
    val DELHI =
        MetroBox(
            name = "Delhi",
            minLat = 28.4046285,
            maxLat = 28.8834464,
            minLon = 76.8388351,
            maxLon = 77.3453379,
        )

    /**
     * Hyderabad city relation 7868535 (Telangana), not the wider HMDA region.
     * https://nominatim.openstreetmap.org/search?city=Hyderabad&state=Telangana&country=India&format=jsonv2&limit=1
     */
    val HYDERABAD =
        MetroBox(
            name = "Hyderabad",
            minLat = 17.2916377,
            maxLat = 17.5608321,
            minLon = 78.2387067,
            maxLon = 78.6223912,
        )

    val METROS = listOf(BENGALURU, MUMBAI, DELHI, HYDERABAD)

    private val MONSOON_MONTHS =
        setOf(Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER)

    fun isEligible(
        places: List<LeaveByPlace>,
        localDateTime: LocalDateTime,
    ): Boolean =
        places.size >= MIN_SAVED_PLACES &&
            isLeaveWindow(localDateTime) &&
            localDateTime.month in MONSOON_MONTHS &&
            places.any { place -> METROS.any { metro -> metro.contains(place.lat, place.lon) } }

    /** 16:00:00 inclusive, 20:00:00 exclusive, in the supplied local date-time. */
    fun isLeaveWindow(localDateTime: LocalDateTime): Boolean =
        localDateTime.hour in WINDOW_START_HOUR until WINDOW_END_HOUR_EXCLUSIVE

    private const val WINDOW_START_HOUR = 16
    private const val WINDOW_END_HOUR_EXCLUSIVE = 20
}
