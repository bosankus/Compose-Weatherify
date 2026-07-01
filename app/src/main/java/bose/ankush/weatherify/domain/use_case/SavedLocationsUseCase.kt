package bose.ankush.weatherify.domain.use_case

import bose.ankush.network.model.SavedLocation
import bose.ankush.network.repository.LocationRepository
import javax.inject.Inject

class SavedLocationsUseCase
    @Inject
    constructor(
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
