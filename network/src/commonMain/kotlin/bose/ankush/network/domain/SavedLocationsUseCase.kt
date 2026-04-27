package bose.ankush.network.domain

import bose.ankush.network.model.SavedLocation
import bose.ankush.network.repository.LocationRepository

/**
 * Use case for managing saved locations.
 * Wraps LocationRepository methods with KMP-compatible interface.
 */
class SavedLocationsUseCase(private val repository: LocationRepository) {
    suspend fun getSavedLocations(): Result<List<SavedLocation>> {
        return repository.getSavedLocations()
    }

    suspend fun saveLocation(name: String, lat: Double, lon: Double): Result<Unit> {
        return repository.saveLocation(name, lat, lon)
    }

    suspend fun deleteLocation(id: String): Result<Unit> {
        return repository.deleteLocation(id)
    }
}
