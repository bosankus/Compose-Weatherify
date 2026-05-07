package bose.ankush.network.repository

import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.model.SavedLocation

/**
 * Repository interface for saved favourite locations.
 * All operations require the user to be authenticated (JWT is attached by the interceptor).
 */
interface LocationRepository {
    /** Save a new favourite location. */
    suspend fun saveLocation(
        name: String,
        lat: Double,
        lon: Double,
    ): Result<Unit>

    /** Retrieve all saved locations for the current user. */
    suspend fun getSavedLocations(): Result<List<SavedLocation>>

    /** Delete a saved location by its server-assigned id. */
    suspend fun deleteLocation(id: String): Result<Unit>

    /** Search for place suggestions by query string. */
    suspend fun searchPlaces(query: String): Result<List<PlaceSuggestion>>
}
