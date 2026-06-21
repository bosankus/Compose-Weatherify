package bose.ankush.network.domain

import bose.ankush.network.model.SavedLocation
import bose.ankush.network.repository.LocationRepository

class SavedLocationsUseCase(
    private val repository: LocationRepository,
) {
    suspend fun getSavedLocations(): Result<List<SavedLocation>> = repository.getSavedLocations()

    suspend fun saveLocation(
        name: String,
        lat: Double,
        lon: Double,
    ): Result<Unit> = repository.saveLocation(name, lat, lon)

    suspend fun deleteLocation(id: String): Result<Unit> = repository.deleteLocation(id)
}
