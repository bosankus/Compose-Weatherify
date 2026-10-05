package bose.ankush.home.presentation.wander

import bose.ankush.finder.domain.model.LocationSuggestion

/** State of the add-a-place sheet's primary button. */
internal enum class WanderAddPlaceAction(
    val enabled: Boolean,
) {
    /** Nothing picked yet: the button waits, disabled. */
    PickFirst(enabled = false),

    /** A new place is picked: saving adds it and shows it. */
    Add(enabled = true),

    /** The picked place is already saved: the button only switches to it. */
    ShowSaved(enabled = true),

    /** A save is in flight. */
    Saving(enabled = false),
    ;

    companion object {
        fun of(
            selected: LocationSuggestion?,
            isSaving: Boolean,
            isSaved: (LocationSuggestion) -> Boolean,
        ): WanderAddPlaceAction =
            when {
                isSaving -> Saving
                selected == null -> PickFirst
                isSaved(selected) -> ShowSaved
                else -> Add
            }

        /** Keeps a pick only while it is still one of the results; a new search clears it. */
        fun retainSelection(
            selected: LocationSuggestion?,
            results: List<LocationSuggestion>,
        ): LocationSuggestion? = selected?.takeIf { it in results }

        /** The + button shows on the places page for premium users, and hides while the sheet is open. */
        fun showAddButton(
            onPlacesPage: Boolean,
            isPremium: Boolean,
            sheetOpen: Boolean,
        ): Boolean = onPlacesPage && isPremium && !sheetOpen
    }
}
