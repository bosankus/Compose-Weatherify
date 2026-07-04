package bose.ankush.finder.presentation.savedlocations

import bose.ankush.finder.domain.model.Location

internal sealed interface SavedLocationsIntent {
    object Load : SavedLocationsIntent

    data class Save(
        val name: String,
        val lat: Double,
        val lon: Double,
    ) : SavedLocationsIntent

    data class Delete(
        val id: String,
    ) : SavedLocationsIntent

    data class SelectLocation(
        val location: Location,
    ) : SavedLocationsIntent

    object MessageShown : SavedLocationsIntent
}
