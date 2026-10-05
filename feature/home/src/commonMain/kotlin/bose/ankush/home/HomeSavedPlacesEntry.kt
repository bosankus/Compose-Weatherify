package bose.ankush.home

/**
 * Cross-feature entry point to Home's own saved places page (the second page of the Android
 * Wander home pager). Lets the app's saved locations tab open that page instead of the
 * standalone saved locations route, without a direct ViewModel reference across modules.
 */
interface HomeSavedPlacesEntry {
    /**
     * Asks Home to show its saved places page. Returns false when this platform's home has no
     * such page, so the caller keeps navigating to the saved locations route.
     */
    fun openSavedPlaces(): Boolean
}
