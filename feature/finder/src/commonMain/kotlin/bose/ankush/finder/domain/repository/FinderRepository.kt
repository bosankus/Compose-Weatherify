package bose.ankush.finder.domain.repository

import bose.ankush.finder.domain.model.Location
import bose.ankush.finder.domain.model.LocationSuggestion

interface FinderRepository {
    suspend fun saveLocation(
        name: String,
        lat: Double,
        lon: Double,
    ): Result<Unit>

    suspend fun getSavedLocations(): Result<List<Location>>

    suspend fun deleteLocation(id: String): Result<Unit>

    suspend fun searchPlaces(query: String): Result<List<LocationSuggestion>>
}
