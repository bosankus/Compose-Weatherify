package bose.ankush.network.repository

import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.model.SavedLocation

interface LocationRepository {
    suspend fun saveLocation(
        name: String,
        lat: Double,
        lon: Double,
    ): Result<Unit>

    suspend fun getSavedLocations(): Result<List<SavedLocation>>

    suspend fun deleteLocation(id: String): Result<Unit>

    suspend fun searchPlaces(query: String): Result<List<PlaceSuggestion>>
}
