package bose.ankush.network.repository

import bose.ankush.network.api.PlaceEventApiService
import bose.ankush.network.model.CreatePlaceEventRequest
import bose.ankush.network.model.PlaceEvent

class PlaceEventRepositoryImpl(
    private val apiService: PlaceEventApiService,
) : PlaceEventRepository {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun getPlaceEvents(
        lat: Double,
        lon: Double,
    ): Result<List<PlaceEvent>> =
        try {
            val response = apiService.getPlaceEvents(lat, lon)
            if (response.status) {
                Result.success(response.data ?: emptyList())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createPlaceEvent(request: CreatePlaceEventRequest): Result<Unit> =
        try {
            val response = apiService.createPlaceEvent(request)
            if (response.status) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
}
