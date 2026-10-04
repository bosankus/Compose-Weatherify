package bose.ankush.network.repository

import bose.ankush.network.model.CreatePlaceEventRequest
import bose.ankush.network.model.PlaceEvent

interface PlaceEventRepository {
    suspend fun getPlaceEvents(
        lat: Double,
        lon: Double,
    ): Result<List<PlaceEvent>>

    suspend fun createPlaceEvent(request: CreatePlaceEventRequest): Result<Unit>
}
