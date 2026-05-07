package bose.ankush.network.api

import bose.ankush.network.model.ApiResponse
import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.model.SaveLocationRequest
import bose.ankush.network.model.SavedLocation

/**
 * API service for saved locations. All endpoints require a valid JWT (handled by auth interceptor).
 */
interface LocationApiService {
    /** POST /save-location — save a favourite location. */
    suspend fun saveLocation(request: SaveLocationRequest): ApiResponse<Unit>

    /** GET /saved-places — retrieve all saved locations for the current user. */
    suspend fun getSavedLocations(): ApiResponse<List<SavedLocation>>

    /** DELETE /saved-places/{id} — remove a saved location by its id. */
    suspend fun deleteLocation(id: String): ApiResponse<Unit>

    /** POST /api/v1/places/search — search for place suggestions by query string. */
    suspend fun searchPlaces(query: String): ApiResponse<List<PlaceSuggestion>>
}
