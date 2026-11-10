package bose.ankush.home.presentation.screen

/** What sits at the right end of a saved places card. */
internal enum class PlaceCardTrailing {
    /** The place Home is showing: a tick, and no remove button. */
    Selected,

    /** Picked, but Home is still fetching its weather: a spinner where the tick will land. */
    Switching,

    /** Any other removable place: the remove (X) button. */
    Remove,

    /** GPS, or a place still being stored: an empty gap. */
    None,
}

internal fun placeCardTrailing(
    isActive: Boolean,
    canDelete: Boolean,
    isSwitching: Boolean = false,
): PlaceCardTrailing =
    when {
        isActive && isSwitching -> PlaceCardTrailing.Switching
        isActive -> PlaceCardTrailing.Selected
        canDelete -> PlaceCardTrailing.Remove
        else -> PlaceCardTrailing.None
    }
