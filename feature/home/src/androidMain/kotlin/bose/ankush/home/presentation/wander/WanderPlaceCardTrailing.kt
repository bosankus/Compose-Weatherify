package bose.ankush.home.presentation.wander

/** What sits at the right end of a saved places card. */
internal enum class WanderPlaceCardTrailing {
    /** The place Home is showing: a tick, and no remove button. */
    Selected,

    /** Any other removable place: the remove (X) button. */
    Remove,

    /** GPS, or a place still being stored: an empty gap. */
    None,
}

internal fun placeCardTrailing(
    isActive: Boolean,
    canDelete: Boolean,
): WanderPlaceCardTrailing =
    when {
        isActive -> WanderPlaceCardTrailing.Selected
        canDelete -> WanderPlaceCardTrailing.Remove
        else -> WanderPlaceCardTrailing.None
    }
