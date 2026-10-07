package bose.ankush.home.presentation.screen

/** What sits at the right end of a saved places card. */
internal enum class PlaceCardTrailing {
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
): PlaceCardTrailing =
    when {
        isActive -> PlaceCardTrailing.Selected
        canDelete -> PlaceCardTrailing.Remove
        else -> PlaceCardTrailing.None
    }
