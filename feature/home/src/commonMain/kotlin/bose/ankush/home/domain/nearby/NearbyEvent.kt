package bose.ankush.home.domain.nearby

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * A place event near the active location. [startsAt] is the start in the device zone and
 * [note] is the optional free text the server keeps with it. A blank [title] stays blank;
 * nothing is filled in.
 */
internal data class NearbyEvent(
    val title: String,
    val startsAt: LocalDateTime,
    val note: String? = null,
) {
    /** The local start date, used for the week strip and the day filter. */
    val date: LocalDate
        get() = startsAt.date
}
