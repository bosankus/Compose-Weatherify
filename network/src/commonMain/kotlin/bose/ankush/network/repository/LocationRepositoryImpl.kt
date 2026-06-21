package bose.ankush.network.repository

import bose.ankush.network.api.LocationApiService
import bose.ankush.network.model.PlaceSuggestion
import bose.ankush.network.model.SaveLocationRequest
import bose.ankush.network.model.SavedLocation

class LocationRepositoryImpl(
    private val apiService: LocationApiService,
) : LocationRepository {
    override suspend fun saveLocation(
        name: String,
        lat: Double,
        lon: Double,
    ): Result<Unit> =
        try {
            val response =
                apiService.saveLocation(SaveLocationRequest(name = name, lat = lat, lon = lon))
            if (response.status) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getSavedLocations(): Result<List<SavedLocation>> =
        try {
            val response = apiService.getSavedLocations()
            if (response.status) {
                Result.success(response.data ?: emptyList())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun deleteLocation(id: String): Result<Unit> =
        try {
            val response = apiService.deleteLocation(id)
            if (response.status) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun searchPlaces(query: String): Result<List<PlaceSuggestion>> =
        try {
            val response = apiService.searchPlaces(query)
            if (response.status) {
                Result.success(response.data ?: emptyList())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
