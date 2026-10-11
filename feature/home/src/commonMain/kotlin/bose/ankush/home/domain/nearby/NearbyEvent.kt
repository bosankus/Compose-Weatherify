package bose.ankush.home.domain.nearby

import kotlinx.datetime.LocalDate

/**
 * A place event near the active location, on its local start date. A blank [title] stays
 * blank; nothing is filled in.
 */
internal data class NearbyEvent(
    val title: String,
    val date: LocalDate,
)
