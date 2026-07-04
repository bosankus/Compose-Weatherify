package bose.ankush.finder.presentation.savedlocations

internal sealed interface SavedLocationsEffect {
    /** Emitted when the user confirms a saved location as their default weather source. */
    data class LocationSelected(
        val lat: Double,
        val lon: Double,
        val name: String,
    ) : SavedLocationsEffect
}
